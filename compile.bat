mkdir classes
call ant clean
call ant compile
javac -cp .;classes;lib/lpsolve55.jar Main.java
pause