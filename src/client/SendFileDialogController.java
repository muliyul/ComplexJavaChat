package client;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Vector;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;

public class SendFileDialogController {
    @FXML
    private ProgressIndicator progressBall;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Button browseButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button sendButton;

    private Stage stage;
    private PrivateChat pc;
    private List<URL> selectedFiles;

    public SendFileDialogController(Stage sfd, PrivateChat pc) {
	selectedFiles = new Vector<>();
	this.pc = pc;
	this.stage = sfd;
    }

    public void browse() {
	showBrowseDialog();
    }

    public void cancelSend() {

    }

    public void initiateSend() throws URISyntaxException, InterruptedException {
	if (selectedFiles.size() > 0) {
	    for (URL f : selectedFiles) {
		pc.sendFile(this, new File(f.toURI()));
	    }
	}

    }

    private void showBrowseDialog() {
	// selectedFiles.add(getClass().getResource("/Resources/MuliChat.ico"));
	selectedFiles.add(getClass().getResource("/Resources/hkj.mp4"));
    }

    public void updatePercentage(double value) {
	Platform.runLater(new Runnable() {
	    public void run() {
		progressBall.setProgress(value);
		progressBar.setProgress(value);
	    }
	});
    }

    protected void close() {
	stage.close();
    }

}
