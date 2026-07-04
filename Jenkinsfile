pipeline {
  agent any

  environment {
    REGISTRY = "docker.io"
    NAMESPACE = "name spece do docker"
    DEPLOY_HOST = "servidor"
    DEPLOY_USER = "root"
    DEPLOY_PATH = "/root/financeflow"
  }

  options {
    timestamps()
    disableConcurrentBuilds()
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Definir versao') {
      steps {
        script {
          def exactTag = sh(
            script: "git describe --tags --exact-match 2>/dev/null || true",
            returnStdout: true
          ).trim()

          if (exactTag) {
            env.APP_VERSION = exactTag.replaceFirst('^v', '')
          } else {
            sh "chmod +x scripts/next-version.sh"
            env.APP_VERSION = sh(
              script: "scripts/next-version.sh ${env.BUILD_NUMBER}",
              returnStdout: true
            ).trim()
          }

          echo "Versao definida: ${env.APP_VERSION}"
        }
      }
    }

    stage('Atualizar versao no POM') {
      steps {
        sh """
          sed -i '/<artifactId>financeflow-api<\\/artifactId>/{ n; s/<version>.*<\\/version>/<version>${env.APP_VERSION}<\\/version>/ }' backend/pom.xml
        """
        echo "backend/pom.xml atualizado para versao ${env.APP_VERSION}"
      }
    }

    stage('Build imagens') {
      steps {
        sh """
          docker build -t ${REGISTRY}/${NAMESPACE}/financeflow-api:${APP_VERSION} -t ${REGISTRY}/${NAMESPACE}/financeflow-api:latest backend
          docker build --build-arg BUILD_VERSION=${APP_VERSION} -t ${REGISTRY}/${NAMESPACE}/financeflow-frontend:${APP_VERSION} -t ${REGISTRY}/${NAMESPACE}/financeflow-frontend:latest frontend
        """
      }
    }

    stage('Push imagens') {
      steps {
        withCredentials([
          usernamePassword(
            credentialsId: 'dockerhub-creds',
            usernameVariable: 'DOCKER_USER',
            passwordVariable: 'DOCKER_PASS'
          )
        ]) {
          sh """
            echo "$DOCKER_PASS" | docker login ${REGISTRY} -u "$DOCKER_USER" --password-stdin
            docker push ${REGISTRY}/${NAMESPACE}/financeflow-api:${APP_VERSION}
            docker push ${REGISTRY}/${NAMESPACE}/financeflow-api:latest
            docker push ${REGISTRY}/${NAMESPACE}/financeflow-frontend:${APP_VERSION}
            docker push ${REGISTRY}/${NAMESPACE}/financeflow-frontend:latest
          """
        }
      }
    }

    stage('Deploy Docker remoto') {
      steps {
        sshagent(credentials: ['deploy-server-ssh']) {
          sh """
            scp -o StrictHostKeyChecking=no docker-compose.prod.yml ${DEPLOY_USER}@${DEPLOY_HOST}:${DEPLOY_PATH}/
            ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} '
              cd ${DEPLOY_PATH} &&
              export REGISTRY=${REGISTRY} &&
              export NAMESPACE=${NAMESPACE} &&
              export APP_VERSION=${APP_VERSION} &&
              docker compose -f docker-compose.prod.yml --env-file .env.prod pull &&
              docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --remove-orphans
            '
          """
        }
      }
    }
  }

  post {
    success {
      echo "Deploy concluido com sucesso. Versao: ${env.APP_VERSION}"
    }
    failure {
      echo "Pipeline falhou. Verifique os logs de build/push/deploy."
    }
    always {
      sh "docker logout ${REGISTRY} || true"
    }
  }
}
