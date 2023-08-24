buildMvn {
  publishModDescriptor = true
  mvnDeploy = true
  buildNode = 'jenkins-agent-java17'

  doDocker = {
    buildJavaDocker {
      publishMaster = true
      healthChk = false
      healthChkCmd = 'curl -sS --fail -o /dev/null http://localhost:8081/admin/health/ || exit 1'
    }
  }
}

