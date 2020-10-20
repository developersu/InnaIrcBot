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
                sh 'mvn package'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }
        stage('Deploy') {
            steps {
            // TODO: consider switch to docker registry
                sh 'cp  ./target/InnaIrcBot-*-jar-with-dependencies.jar /rel/InnaIrcBot.jar'
                // sh 'docker restart innaircbot'
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: 'target/*-jar-with-dependencies.jar', onlyIfSuccessful: true
        }
    }
}