mkdir classes
call ant clean
call ant compile
javac -cp .;classes;lib/lpsolve55j.jar Main.java
pause