pipeline {
    agent any
    tools { jdk 'jdk21'; maven 'maven3' }

    stages {
        stage('Checkout') { steps { checkout scm } }

        stage('Build & Test') {
            steps { sh 'mvn -B verify' }
            post { always { junit 'target/surefire-reports/*.xml' } }
        }

        stage('Image') {
            steps { sh "docker build -t ies-eligibility-platform:${env.GIT_COMMIT} ." }
        }

        // Canary first, watch, then widen. Each service carries its own
        // rollback plan: redeploy the previous image tag.
        stage('Deploy canary') {
            when { branch 'main' }
            steps { sh "kubectl set image deploy/ies-eligibility-canary app=ies-eligibility-platform:${env.GIT_COMMIT}" }
        }
    }
}
