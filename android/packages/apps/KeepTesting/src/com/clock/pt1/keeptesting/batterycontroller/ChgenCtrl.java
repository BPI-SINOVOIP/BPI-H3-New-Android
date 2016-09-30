package com.clock.pt1.keeptesting.batterycontroller;


import android.util.Log;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;


public class ChgenCtrl {
	private Shell shell = null;
	private int result = -1;
	private CommandCapture command = null;
	
	public ChgenCtrl(Shell shell) {
		this.shell = shell;
	}
	private void commandWait(Command command) {
        while (!command.isFinished()) {
            synchronized (command) {
                try {
                    if (!command.isFinished()) {
                    	command.wait(2000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!command.isExecuting() && !command.isFinished()) {
                if (!shell.isExecuting && !shell.isReading) {
                    Log.e(BatteryControllerActivity.TAG, "Waiting for a command to be executed in a shell that is not executing and not reading! \n\n Command: " + command.getCommand());
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                } else if (shell.isExecuting && !shell.isReading) {
                    Log.e(BatteryControllerActivity.TAG, "Waiting for a command to be executed in a shell that is executing but not reading! \n\n Command: " + command.getCommand());
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                } else {
                    Log.e(BatteryControllerActivity.TAG, "Waiting for a command to be executed in a shell that is not reading! \n\n Command: " + command.getCommand());
                    Exception e = new Exception();
                    e.setStackTrace(Thread.currentThread().getStackTrace());
                    e.printStackTrace();
                }
            }

        }
	}
	
	public int getValue() {
	    command = new CommandCapture(0, false, "cat /sys/class/power_supply/battery/chgen") {
	        @Override
	        public void output(int id, String line) {
	        	if(result == -1) {
	        		int value = -1;
	        		try {
	        			value = Integer.parseInt(line);
	        		} catch (NumberFormatException e2) {
						Log.i(BatteryControllerActivity.TAG, "Invalid number format!!");
						value = -1;
					}
	        		result = value;
	        	}
	        }
	    };
	    try {
			shell.add(command);
			commandWait(command);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return result;
	}

	public void setValue(int value) {
		try {
            command = new CommandCapture(0, false, "echo " + value + " > /sys/class/power_supply/battery/chgen");
            RootTools.getShell(true).add(command);
            commandWait(command);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
}
