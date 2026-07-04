#!/bin/bash
# Script para configurar Java 17 para o projeto FinanceFlow

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

echo "Java configurado:"
java -version

echo ""
echo "Para usar permanentemente, adicione ao seu ~/.bashrc ou ~/.zshrc:"
echo "export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64"
echo "export PATH=\$JAVA_HOME/bin:\$PATH"
