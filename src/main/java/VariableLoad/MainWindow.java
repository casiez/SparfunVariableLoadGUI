/*
	Sparkfun Variable Load GUI
	Interface for https://www.sparkfun.com/products/14449
	Gery Casiez
*/

package VariableLoad;

import java.util.HashMap;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainWindow extends Application {
	SerialPort comPort;
	
	String isource = "0.0", ilimit = "0.0", vsource = "0.0", vmin = "0.0", maHours = "0.0";
	Label isourceLbl, ilimitLbl, vsourceLbl, vminLbl, maHoursLbl, powerLbl;
	Label ilimitValToSet, vminValToSet;
	Slider ilimitSlider, vminSlider;
    String s0 = "";
    Label status = new Label("Not connected");
    HashMap<CheckMenuItem, SerialPort> menuItemSerialPortHM = new HashMap<CheckMenuItem, SerialPort>();
    CheckMenuItem currentMenuItem = null;
    Preferences prefs = Preferences.userRoot().node("Sparkfun Variable Load GUI");
    boolean portJustOpened = false;
	
	public void start(Stage stage) {
		MenuBar menuBar = new MenuBar();
		Menu menuSerialPorts = new Menu("Serial ports");
		menuBar.getMenus().addAll(menuSerialPorts);
		String previouslyUsedPort = prefs.get("port", "");
		
		for (SerialPort s: SerialPort.getCommPorts()) {
			String portName = s.getSystemPortName() + "(" + s.getDescriptivePortName() + ")";
			CheckMenuItem menuItemSerialPort = new CheckMenuItem(portName);
			if (portName.equals(previouslyUsedPort)) {
				menuItemSerialPort.setSelected(true);
				comPort = s;
				openPort();
			}
			menuItemSerialPortHM.put(menuItemSerialPort,s);
			menuSerialPorts.getItems().add(menuItemSerialPort);
			
			menuItemSerialPort.setOnAction(e -> {
				if (currentMenuItem != null) {
					currentMenuItem.setSelected(false);
					menuItemSerialPortHM.get(currentMenuItem).closePort();
				}
				currentMenuItem = (CheckMenuItem)e.getSource();
				comPort = menuItemSerialPortHM.get((CheckMenuItem)e.getSource());
				String serialPortName = comPort.getSystemPortName() + "(" + comPort.getDescriptivePortName() + ")";
				prefs.put("port", serialPortName);
				openPort();
			});
		}

		
		ToggleButton tbON = new ToggleButton("ON");
		tbON.setOnAction(e -> {
			String s = "E1\n";
			int nbWritten = comPort.writeBytes(s.getBytes(), s.length());
	    	if (nbWritten < 0) status.setText("Pb writting to port.");
		});
		ToggleButton tbOFF = new ToggleButton("OFF");
		tbOFF.setOnAction(e -> {
			String s = "E0\n";
			int nbWritten = comPort.writeBytes(s.getBytes(), s.length());
			if (nbWritten < 0) status.setText("Pb writting to port.");
		});		
		ToggleGroup group = new ToggleGroup();
		tbON.setToggleGroup(group);
		tbOFF.setToggleGroup(group);
		tbOFF.setSelected(true);
		HBox hbONOFF = new HBox(tbON,tbOFF);
		HBox.setHgrow(tbON, Priority.ALWAYS);
		HBox.setHgrow(tbOFF, Priority.ALWAYS);
		tbON.setMaxWidth(Double.MAX_VALUE);
		tbOFF.setMaxWidth(Double.MAX_VALUE);

		Label ilimlb = new Label("I limit (A)");
		ilimlb.setTooltip(new Tooltip("The current limit set for the load."));
		ilimitSlider = new Slider(0,4,0);
		ilimitSlider.valueProperty().addListener(new ChangeListener<Object>() {
			@Override
			public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
				ilimitValToSet.setText("" + (Math.round((Double)newValue*1000.0)/1000.0));
			}
		});
		ilimitSlider.setShowTickMarks(true);
		ilimitSlider.setShowTickLabels(true);
		ilimitSlider.setMajorTickUnit(1.0f);
		ilimitSlider.setBlockIncrement(0.1f);
		ilimitSlider.setMaxWidth(Double.MAX_VALUE);
		ilimitValToSet = new Label("0.0");
		ilimitValToSet.setMaxWidth(40);
		Button btSetilim = new Button("Set");
		btSetilim.setOnAction(e -> {
			String s = "I" + ilimitValToSet.getText() + "\n";
			int nbWritten = comPort.writeBytes(s.getBytes(), s.length());
			System.out.println("" + nbWritten + " bytes written");
		});
		ilimitLbl = new Label("0.0");
		ilimitLbl.setMaxWidth(40);
		HBox hbilim = new HBox(ilimlb, ilimitSlider, ilimitValToSet, btSetilim, ilimitLbl);
		hbilim.setAlignment(Pos.CENTER_LEFT);
		hbilim.setSpacing(10);
		HBox.setHgrow(ilimitSlider, Priority.ALWAYS);
		HBox.setHgrow(ilimitValToSet, Priority.ALWAYS);
		HBox.setHgrow(ilimitLbl, Priority.ALWAYS);
		
		Label vminlb = new Label("V min (V)");
		vminlb.setTooltip(new Tooltip("The current minimum voltage before the load cuts out"));
		vminSlider = new Slider(0,30,0);
		vminSlider.valueProperty().addListener(new ChangeListener<Object>() {
			@Override
			public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
				vminValToSet.setText("" + (Math.round((Double)newValue*1000.0)/1000.0));
			}
		});
		vminSlider.setShowTickMarks(true);
		vminSlider.setShowTickLabels(true);
		vminSlider.setMajorTickUnit(1.0f);
		vminSlider.setBlockIncrement(0.1f);
		vminSlider.setMaxWidth(Double.MAX_VALUE);
		vminValToSet = new Label("0.0");
		vminValToSet.setStyle("-fx-font-size:14px;"
			       + "-fx-font-weight: bold");
		vminValToSet.setMaxWidth(40);
		Button btSetvmin = new Button("Set");
		btSetvmin.setOnAction(e -> {
			String s = "V" + vminValToSet.getText() + "\n";
			int nbWritten = comPort.writeBytes(s.getBytes(), s.length());
			if (nbWritten < 0) status.setText("Pb writting to port.");
		});
		vminLbl = new Label("0.0");
		vminLbl.setStyle("-fx-font-size:14px;"
				       + "-fx-font-weight: bold");
		vminLbl.setMaxWidth(40);
		HBox hbvmin = new HBox(vminlb, vminSlider, vminValToSet, btSetvmin, vminLbl);
		hbvmin.setAlignment(Pos.CENTER_LEFT);
		hbvmin.setSpacing(10);
		HBox.setHgrow(vminSlider, Priority.ALWAYS);
		HBox.setHgrow(vminLbl, Priority.ALWAYS);
		HBox.setHgrow(vminValToSet, Priority.ALWAYS);
	
		isourceLbl = new Label("");
		isourceLbl.setMaxWidth(Double.MAX_VALUE);
		isourceLbl.setStyle("-fx-font-size:20px;");
		isourceLbl.setTooltip(new Tooltip("The actual current being drawn from the source."));

		vsourceLbl = new Label("");	
		vsourceLbl.setMaxWidth(Double.MAX_VALUE);
		vsourceLbl.setStyle("-fx-font-size:20px;");
		HBox hbIsourceVSource = new HBox(isourceLbl, vsourceLbl);
		HBox.setHgrow(isourceLbl, Priority.ALWAYS);
		HBox.setHgrow(vsourceLbl, Priority.ALWAYS);
		
		powerLbl = new Label("0.0 W");
		powerLbl.setStyle("-fx-font-size:16px;");
		
		maHoursLbl = new Label("0.0 mAH");
		maHoursLbl.setMaxWidth(Double.MAX_VALUE);
		maHoursLbl.setTooltip(new Tooltip("The number of milliamp hours drawn from the source since it was last reset."));
		 
		Button btresetmaH = new Button("Reset mAH");
		btresetmaH.setMaxWidth(Double.MAX_VALUE);
		btresetmaH.setOnAction(e -> {
			String s = "R\n";
			int nbWritten = comPort.writeBytes(s.getBytes(), s.length());
			if (nbWritten < 0) status.setText("Pb writting to port.");
		});
		HBox hbmah = new HBox(maHoursLbl, btresetmaH);
		HBox.setHgrow(maHoursLbl, Priority.ALWAYS);
		HBox.setHgrow(btresetmaH, Priority.ALWAYS);
		hbmah.setAlignment(Pos.CENTER_LEFT);
		
		VBox main = new VBox();
		main.getChildren().addAll(hbONOFF, hbilim, hbvmin, hbIsourceVSource, powerLbl, hbmah);
		
		BorderPane root = new BorderPane();
		root.setPadding(new Insets(10, 10, 10, 10));
		root.setTop(menuBar);
		root.setCenter(main);
		root.setBottom(status);
		
	    stage.setOnCloseRequest( event ->
	    {
	    	if (comPort != null) comPort.closePort();
	    });
		
		Scene scene = new Scene(root, 500, 250);
		stage.setTitle("Sparkfun Variable Load GUI");
		stage.setScene(scene);
		stage.show();
	}
	
	void openPort() {
		String serialPortName = comPort.getSystemPortName() + "(" + comPort.getDescriptivePortName() + ")";
		if (!comPort.openPort()) {
			status.setText("Can't open port " + serialPortName);
		} else {
			status.setText(serialPortName + " connected.");
			comPort.addDataListener(new SerialPortDataListener() {
				   @Override
				   public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_AVAILABLE; }
				   @Override
				   public void serialEvent(SerialPortEvent event)
				   {
				      if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
				         return;
				      if (comPort.bytesAvailable() > 0) {
				    	  byte[] newData = new byte[comPort.bytesAvailable()];
				    	  int numRead = comPort.readBytes(newData, newData.length);
				    	  if (numRead < 0)
				    	  	status.setText("Pb reading port");
					      String s = new String(newData);
					      s0 = analyze(s0) + s;
				      }
				   }
				});
			portJustOpened = true;
		
		}
	}	
	
	public String analyze(String s) {
		Pattern pattern = Pattern.compile("(?<=\\[)(\\d);12([^\\[]+)\\[");
        Matcher matcher = pattern.matcher(s);
		
        int maxid = 0;
        while(matcher.find()) {
            String val0 = matcher.group(2);
    		Pattern pattern2 = Pattern.compile("(\\d+\\.?\\d+)");
            Matcher matcher2 = pattern2.matcher(val0);
            String val = "";
            while(matcher2.find()) {
            	val = matcher2.group(1);
            }
            switch (Integer.parseInt(matcher.group(1))) {
            	case 1:	isource = val;
            			break;
            	case 2:	ilimit = val;
    					break;
            	case 3:	vsource = val;
						break;
            	case 4:	vmin = val;
    					break;
            	case 5:	maHours = val;  			
    					break;
            }
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                	isourceLbl.setText("Is = " + isource + "A");
                	ilimitLbl.setText(ilimit);
                	if (portJustOpened)
                		ilimitSlider.setValue(Double.parseDouble(ilimit));
                	vsourceLbl.setText("Vs = " + vsource + "V");
                	powerLbl.setText("Power = " + Math.round((Double.parseDouble(isource) * Double.parseDouble(vsource))*100.0)/100.0 + " W");
                	vminLbl.setText(vmin);
                	if (portJustOpened)
                		vminSlider.setValue(Double.parseDouble(vmin));
                	maHoursLbl.setText(maHours + " mAH");
                	portJustOpened = false;
                }
            });
            
            if (matcher.end() > maxid) maxid = matcher.end();
        }
        
        return s.substring(Math.max(0, maxid-1));
	}
	
	public static void main(String[] args) {
		Application.launch(args);
	}
}
