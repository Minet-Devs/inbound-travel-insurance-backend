pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'uat', 'production'],
            description: 'Select deployment environment',
        )
    }
    
    environment {
        ACR_NAME = "hcsbin.azurecr.io"
        SERVICE_NAME = "scheme"
        IMAGE_REPO = "${ACR_NAME}/${env.SERVICE_NAME}"
        REMOTE_DIR = "/home/devserver"
        DOCKER_REGISTRY_CREDENTIALS = 'acr-creds'
        ADMIN_EMAIL = "hussein.mishobo@minet.co.ke"
    }

    stages {
        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Initialize environment') {
            steps {
                script {
                    env.ENVIRONMENT = params.ENVIRONMENT ?: 'dev'
                    echo "Deploy to: ${env.ENVIRONMENT}"
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    def targetBranch = getBranchForEnvironment()
                    echo "Checking out branch: ${targetBranch}"

                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: "*/${targetBranch}"]],
                        extensions: [[$class: 'CleanCheckout']],
                        userRemoteConfigs: [[
                            url: 'https://dev.azure.com/MedicareSystem/Healthcare/_git/scheme',
                            credentialsId: 'azure-devops-pat'
                        ]]
                    ])
                }
            }
        }

        stage('Compute Version Tags') {
            steps {
                script {
                    def COMMIT_SHA = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                    def DATE_YYYYMMDD = sh(script: "date +%Y%m%d", returnStdout: true).trim()
                    env.AUTHOR_NAME = sh(script: "git log -1 --pretty=format:'%an'", returnStdout: true).trim()
                    env.AUTHOR_EMAIL = sh(script: "git log -1 --pretty=format:'%ae'", returnStdout: true).trim()
                    env.COMMIT_MESSAGE = sh(script: "git log -1 --pretty=%s",returnStdout: true).trim()
                    env.VERSION_TAG_DATE_BUILD = "${DATE_YYYYMMDD}.${env.BUILD_NUMBER}"
                    echo "Date with Build: ${VERSION_TAG_DATE_BUILD}"
                    echo "Commit Author: ${env.AUTHOR_NAME}"
                    echo "Author Email: ${env.AUTHOR_EMAIL}"
                }
            }
        }

        stage('Build JAR') {
            agent {
                docker {
                    image 'maven:3.9.9-eclipse-temurin-17'
                    args '-v $HOME/.m2:/root/.m2'
                    reuseNode true
                }
            }
            steps {
                script {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: DOCKER_REGISTRY_CREDENTIALS,
                        usernameVariable: 'ACR_USER',
                        passwordVariable: 'ACR_PASS'
                    )]) {
                        sh """
                            echo $ACR_PASS | docker login ${ACR_NAME} -u $ACR_USER --password-stdin || true
                            docker build -t ${IMAGE_REPO}:${VERSION_TAG_DATE_BUILD} .
                        """
                    }
                }
            }
        }

        stage('Push to ACR') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: DOCKER_REGISTRY_CREDENTIALS,
                    usernameVariable: 'ACR_USER',
                    passwordVariable: 'ACR_PASS'
                )]) {
                    sh """
                        echo $ACR_PASS | docker login ${ACR_NAME} -u $ACR_USER --password-stdin
                        docker push ${IMAGE_REPO}:${VERSION_TAG_DATE_BUILD}
                        docker system prune -a -f
                    """
                }
            }
        }

        stage('Deploy to VM') {
            steps {
                script {
                    def remoteUser = getRemoteUser()
                    echo " ${env.ENVIRONMENT} user is : ${remoteUser}"

                    def remoteHost = getRemoteHost()
                    echo " ${env.ENVIRONMENT} host is : ${remoteHost}"

                    sshagent(['ssh-credentials']) {
                        sh """
                            # Save the version we're about to deploy
                            ssh -o StrictHostKeyChecking=no ${remoteUser}@${remoteHost} \
                                "sed -i 's|^SCHEME_IMAGE_TAG=.*|SCHEME_IMAGE_TAG=${VERSION_TAG_DATE_BUILD}|' .env"

                            # Deploy
                             ssh -o StrictHostKeyChecking=no ${remoteUser}@${remoteHost} \
                                 "sudo docker compose up -d ${env.SERVICE_NAME} --build --remove-orphans"

                             # clean up
                             ssh -o StrictHostKeyChecking=no ${remoteUser}@${remoteHost} \
                                "sudo docker image prune -a -f"
                        """
                    }
                }
            }
        }
    }
    post {
        success {
            script {
                echo "Deployment to ${env.ENVIRONMENT} completed successfully!"
                emailext(
                    subject: "✅ Deployment to ${env.ENVIRONMENT} server completed successfully!🚀",
                    mimeType: 'text/html; charset=UTF-8',
                    body: """
                        <html>
                            <body>
                                <p>Hi ${env.AUTHOR_NAME} 👋,</p>
                                 <p>✅ <b>${SERVICE_NAME} service deployed to ${params.ENVIRONMENT} successfully!🚀</b></p>
                                 <ul>
                                    <li><b>Image Tag:</b> ${env.VERSION_TAG_DATE_BUILD}</li>
                                    <li><b>Commit:</b> ${env.COMMIT_MESSAGE}</li>
                                 </ul>
                                 <p>⏳ Please wait <b>2 minutes</b> for Eureka registration.</p>
                                 <p>
                                   Kind Regards,<br/>
                                   Jenkins ${env.SERVICE_NAME} Pipeline,<br/>
                                   <b>Happy coding 😎</b>
                                 </p>
                            </body>
                        </html>
                    """,
                    to: "${env.AUTHOR_EMAIL},${env.ADMIN_EMAIL}"
                )
            }
        }
        failure {
            script {
                echo "Deployment to ${env.ENVIRONMENT} failed!"
                emailext(
                    subject: "❌ Deployment to ${env.ENVIRONMENT} failed! 💥",
                    mimeType: 'text/html; charset=UTF-8',
                    body: """
                        <html>
                            <body>
                                <p>Hi ${env.AUTHOR_NAME} 👋,</p>
                                 <p>❌ <b>${env.SERVICE_NAME} service deployment to dev server failed!💥</b></p>
                                 <p> Kindly check Jenkins console output for details</p>
                                 <p>
                                  Kind Regards,<br/>
                                  Jenkins ${env.SERVICE_NAME} Pipeline,<br/>
                                  <b>Happy coding 😎</b>
                                 </p>
                            </body>
                        </html>
                    """,
                    to: "${env.AUTHOR_EMAIL},${env.ADMIN_EMAIL}"
                )
            }
        }
    }
}

def getBranchForEnvironment() {
    switch(env.ENVIRONMENT) {
        case 'dev':
            return 'dev'
        case 'uat':
            return 'uat'
        case 'production':
            return 'production'
        default:
            return 'dev'
    }
}

def getRemoteUser() {
    switch(env.ENVIRONMENT) {
        case 'dev':
            return 'devserver'
        case 'uat':
            return 'uathealthcare'
        case 'production':
            return 'prodserver'
        default:
            return 'devserver'
    }
}

def getRemoteHost() {
    switch(env.ENVIRONMENT) {
        case 'dev':
            return '10.1.0.5'
        case 'uat':
            return '10.1.0.6'
        case 'production':
            return '10.1.3.4'
        default:
            return '10.1.0.5'
    }
}