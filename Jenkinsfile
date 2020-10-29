pipeline {
    agent {
        docker {
           image 'maven:3-alpine'
            args '-v /home/docker/jenkins/files/m2:/root/.m2 -v /home/docker/innaircbot/files/artifact:/rel'
        }
    }

    stages {
        stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar, target/*.exe'
                }
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        stage('Deploy') {
            steps {
                sh 'cp  ./target/InnaIrcBot-*-jar-with-dependencies.jar /rel/InnaIrcBot.jar'
                // TODO: consider switch to docker registry
                // sh 'docker restart innaircbot'
            }
        }
    }
}