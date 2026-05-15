rm -rf out
mkdir -p out

javac -cp junit-platform-console-standalone-1.11.4.jar -d out $(find src -name "*.java") Homework2Test.java

java -jar junit-platform-console-standalone-1.11.4.jar execute --class-path out --select-class ija.ija2025.homework2.Homework2Test