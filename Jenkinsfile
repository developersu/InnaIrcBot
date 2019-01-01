pipeline {
    agent {
        docker {
           image 'maven:3-alpine'
            args '-v /root/.m2:/root/.m2 -v /home/docker/innaircbot/files/artifact:/rel'
        }
    }

    stages {
        stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
        }
        stage('Test') {
            steps {
                echo 'Skip testing...'
            }
        }
        stage('Deploy') {
            steps {
                sh 'cp  ./target/InnaIrcBot-*-jar-with-dependencies.jar /rel/InnaIrcBot.jar'
                // sh 'docker restart innaircbot'
            }
        }
    }
}