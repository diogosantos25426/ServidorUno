module org.example.clienteuno {
	requires javafx.controls;
	requires javafx.fxml;


	opens org.example.clienteuno to javafx.fxml;
	exports org.example.clienteuno;
}