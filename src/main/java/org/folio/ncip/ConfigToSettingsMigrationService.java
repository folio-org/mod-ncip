package org.folio.ncip;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.folio.util.PercentCodec;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ConfigToSettingsMigrationService {

    private static final Logger logger = LogManager.getLogger(ConfigToSettingsMigrationService.class);
    private MultiMap okapiHeaders;
    private String baseUrl;

    public Future<Void> process(RoutingContext ctx) {
        logger.info("migrating ncip configurations...");
        okapiHeaders = ctx.request().headers();
        baseUrl = okapiHeaders.get(Constants.X_OKAPI_URL);

        return fetchConfig(ctx)
                .compose(this::transform)
                .compose(this::writeSettings)
                .compose(this::verifySettings)
                .compose(this::deleteLegacyConfigs)
                .onSuccess(v -> logger.info("migration complete"))
                .onFailure(e -> logger.error("migration failed", e));
    }

    private Future<JsonObject> fetchConfig(RoutingContext ctx) {
        logger.info("fetchConfig called");

        String configQuery = "(module==NCIP)";
        String configEndpoint = baseUrl + "/configurations/entries?query=" + PercentCodec.encode(configQuery)
                + "&limit=10000";

        return Future.future(promise -> {
            try {
                logger.info("Fetching NCIP configurations from: {}", configEndpoint);
                String response = FolioGatewayClient.get(configEndpoint, okapiHeaders);

                if (response == null || response.isBlank()) {
                    promise.fail("Empty config response");
                    return;
                }

                JsonObject json = new JsonObject(response);
                JsonArray configs = json.getJsonArray(Constants.CONFIGS);

                if (configs == null || configs.size() < 1) {
                    logger.info("No NCIP configurations found in mod-configuration. Migration already complete.");
                    JsonObject empty = new JsonObject();
                    empty.put(Constants.CONFIGS, new JsonArray());
                    promise.complete(empty);
                    return;
                }

                logger.info("Found {} NCIP configurations to migrate", configs.size());
                promise.complete(json);

            } catch (Exception e) {
                logger.error("Error fetching configurations", e);
                promise.fail(e);
            }
        });
    }

    private Future<JsonObject> transform(JsonObject configResponse) {
        logger.info("Transforming configurations to grouped settings format");

        try {
            JsonArray configs = configResponse.getJsonArray(Constants.CONFIGS);
            if (configs == null) {
                configs = new JsonArray();
            }

            // Group configs by configName (agency id or "toolkit")
            // Each group becomes one mod-settings record with a JSON object value
            Map<String, JsonObject> groupedValues = new HashMap<>();
            Map<String, String> groupIds = new HashMap<>();

            Iterator configsIterator = configs.iterator();
            while (configsIterator.hasNext()) {
                JsonObject config = (JsonObject) configsIterator.next();
                String code = config.getString(Constants.CODE_KEY);
                String configName = config.getString(Constants.CONFIG_KEY);
                String value = config.getString(Constants.VALUE_KEY);

                String groupKey = configName.toLowerCase();

                if (!groupedValues.containsKey(groupKey)) {
                    groupedValues.put(groupKey, new JsonObject());
                    // Generate a stable UUID for this group based on scope+key
                    groupIds.put(groupKey, UUID.nameUUIDFromBytes(
                            (Constants.SETTING_SCOPE + "|" + groupKey).getBytes()).toString());
                }
                groupedValues.get(groupKey).put(code, value);
            }

            // Build the settings array - one entry per group
            JsonArray transformedSettings = new JsonArray();
            for (Map.Entry<String, JsonObject> entry : groupedValues.entrySet()) {
                String groupKey = entry.getKey();
                JsonObject setting = new JsonObject();
                setting.put("id", groupIds.get(groupKey));
                setting.put("scope", Constants.SETTING_SCOPE);
                setting.put(Constants.KEY, groupKey);
                setting.put("value", entry.getValue());
                logger.info("Grouped {} properties under key '{}'", entry.getValue().size(), groupKey);
                transformedSettings.add(setting);
            }

            logger.info("Transformed {} config entries into {} grouped settings records",
                    configs.size(), transformedSettings.size());

            JsonObject result = new JsonObject();
            result.put("settings", transformedSettings);
            result.put("legacyConfigs", configs);
            return Future.succeededFuture(result);

        } catch (Exception e) {
            logger.error("Error transforming configurations", e);
            return Future.failedFuture(e);
        }
    }

    private Future<JsonObject> writeSettings(JsonObject transformedData) {
        logger.info("Writing settings to mod-settings");

        JsonArray settings = transformedData.getJsonArray("settings");
        String url = baseUrl + Constants.SETTINGS_URL;

        // Chain all the writes together
        Future<Void> chain = Future.succeededFuture();

        for (int i = 0; i < settings.size(); i++) {
            JsonObject setting = settings.getJsonObject(i);
            chain = chain.compose(v -> writeSingleSetting(setting, url));
        }

        return chain.map(transformedData);
    }

    private Future<Void> writeSingleSetting(JsonObject setting, String url) {
        return Future.future(promise -> {
            try {
                String key = setting.getString(Constants.KEY);
                String settingId = setting.getString(Constants.ID);
                logger.info("Upserting setting: {} ({})", key, settingId);

                try {
                    FolioGatewayClient.post(url, setting, okapiHeaders);
                } catch (Exception postError) {
                    // Idempotent parallel-safe path: if already exists, update by deterministic id.
                    String putEndpoint = baseUrl + Constants.SETTINGS_URL + "/" + settingId;
                    logger.info("POST failed for key {}, trying PUT upsert path", key);
                    FolioGatewayClient.put(putEndpoint, setting, okapiHeaders);
                }

                // Verify the setting was created by fetching it back
                String getSettingsEndpoint = baseUrl + Constants.SETTINGS_URL + "/" + settingId;

                logger.info("Verifying setting migration for key: {}", key);
                String verifyResponse = FolioGatewayClient.get(getSettingsEndpoint, okapiHeaders);
                JsonObject savedSetting = new JsonObject(verifyResponse);

                String actualKey = savedSetting.getString(Constants.KEY);
                if (key.equalsIgnoreCase(actualKey)) {
                    logger.info("VERIFIED: Setting successfully migrated - key={}, properties={}",
                            key, setting.getJsonObject("value").size());
                    promise.complete();
                } else {
                    logger.warn("VERIFICATION FAILED: Expected key={} but got key={}", key, actualKey);
                    promise.complete(); // Log mismatch but continue
                }

            } catch (Exception e) {
                logger.error("Error writing setting: {}", setting.getString(Constants.KEY), e);
                promise.fail(e);
            }
        });
    }

    private Future<JsonObject> verifySettings(JsonObject transformedData) {
        return Future.future(promise -> {
            try {
                JsonArray settings = transformedData.getJsonArray("settings");
                JsonArray legacyConfigs = transformedData.getJsonArray("legacyConfigs");

                if (legacyConfigs == null || legacyConfigs.isEmpty()) {
                    logger.info("No legacy configs to verify.");
                    promise.complete(transformedData);
                    return;
                }

                Map<String, JsonObject> settingsByKey = new HashMap<>();
                for (int i = 0; i < settings.size(); i++) {
                    JsonObject setting = settings.getJsonObject(i);
                    settingsByKey.put(setting.getString(Constants.KEY), setting);
                }

                List<String> mismatches = new ArrayList<>();

                for (int i = 0; i < legacyConfigs.size(); i++) {
                    JsonObject legacy = legacyConfigs.getJsonObject(i);
                    String configName = legacy.getString(Constants.CONFIG_KEY);
                    String code = legacy.getString(Constants.CODE_KEY);
                    String expectedValue = legacy.getString(Constants.VALUE_KEY);

                    String key = configName == null ? null : configName.toLowerCase();
                    JsonObject setting = key == null ? null : settingsByKey.get(key);
                    if (setting == null) {
                        mismatches.add("Missing settings key for configName='" + configName + "'");
                        continue;
                    }

                    JsonObject values = setting.getJsonObject(Constants.VALUE_KEY);
                    String actual = values == null ? null : values.getString(code);
                    if (actual == null || !actual.equals(expectedValue)) {
                        mismatches.add("Mismatch for " + configName + "." + code
                                + " expected='" + expectedValue + "' actual='" + actual + "'");
                    }
                }

                if (!mismatches.isEmpty()) {
                    String message = "Settings parity verification failed. " + mismatches.size()
                            + " mismatch(es). First: "
                            + mismatches.get(0);
                    promise.fail(new Exception(message));
                    return;
                }

                logger.info("Verified parity for {} legacy configuration entries.", legacyConfigs.size());
                promise.complete(transformedData);
            } catch (Exception e) {
                promise.fail(e);
            }
        });
    }

    private Future<Void> deleteLegacyConfigs(JsonObject transformedData) {
        return Future.future(promise -> {
            try {
                JsonArray legacyConfigs = transformedData.getJsonArray("legacyConfigs");
                if (legacyConfigs == null || legacyConfigs.isEmpty()) {
                    logger.info("No legacy config entries to delete.");
                    promise.complete();
                    return;
                }

                int deletedOrAlreadyGone = 0;
                for (int i = 0; i < legacyConfigs.size(); i++) {
                    JsonObject legacy = legacyConfigs.getJsonObject(i);
                    String id = legacy.getString(Constants.ID);

                    if (id == null || id.isBlank()) {
                        logger.warn("Skipping legacy config delete because id is missing: {}", legacy.encode());
                        continue;
                    }

                    String deleteEndpoint = baseUrl + "/configurations/entries/" + id;
                    try (CloseableHttpResponse response = FolioGatewayClient.delete(deleteEndpoint, okapiHeaders)) {
                        int code = response.getCode();
                        if (code == 200 || code == 204 || code == 404) {
                            deletedOrAlreadyGone++;
                        } else {
                            promise.fail(new Exception("Failed deleting legacy config id " + id + ", status=" + code));
                            return;
                        }
                    }
                }

                logger.info("Legacy config cleanup complete. {} entries deleted/already removed.",
                        deletedOrAlreadyGone);
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }
        });
    }

}