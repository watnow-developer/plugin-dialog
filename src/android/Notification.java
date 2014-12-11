/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package jp.watnow.plugins.dialog;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;


/**
 * This class provides access to notifications on the device.
 *
 * Be aware that this implementation gets called on 
 * navigator.notification.{alert|confirm|prompt}, and that there is a separate
 * implementation in org.apache.cordova.CordovaChromeClient that gets
 * called on a simple window.{alert|confirm|prompt}.
 */
public class Notification extends CordovaPlugin {

    public int confirmResult = -1;
    public ProgressDialog spinnerDialog = null;
    public ProgressDialog progressDialog = null;
    final static String INPUT_SECURE = "secure";
    final static String INPUT_NORMAL = "normal";

    /**
     * Constructor.
     */
    public Notification() {
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArray of arguments for the plugin.
     * @param callbackContext   The callback context used when calling back into JavaScript.
     * @return                  True when the action was valid, false otherwise.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    	/*
    	 * Don't run any of these if the current activity is finishing
    	 * in order to avoid android.view.WindowManager$BadTokenException
    	 * crashing the app. Just return true here since false should only
    	 * be returned in the event of an invalid action.
    	 */
    	if(this.cordova.getActivity().isFinishing()) return true;
    	
    	if (action.equals("alert")) {
            this.alert(args.getString(0), args.getString(1), args.getString(2), callbackContext);
            return true;
        }
        else if (action.equals("confirm")) {
            this.confirm(args.getString(0), args.getString(1), args.getJSONArray(2), callbackContext);
            return true;
        }
        else if (action.equals("prompt")) {
        	String type = INPUT_NORMAL;
        	if(args.getString(4).equals(INPUT_SECURE)){
        		type = INPUT_SECURE;
        	}
            this.prompt(args.getString(0), args.getString(1), args.getJSONArray(2), args.getString(3),type, callbackContext);
            return true;
        }
        else if (action.equals("list")) {
            this.list(args.getString(0), args.getJSONArray(1), callbackContext);
            return true;
        } else if (action.equals("login")) {
            this.login(args.getString(0), args.getString(1), args.getJSONArray(2), args.getJSONArray(3), callbackContext);
            return true;
        }
        else {
            return false;
        }

        // Only alert and confirm are async.
        //callbackContext.success();
        //return true;
    }

    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------

    /**
     * Builds and shows a native Android alert with given Strings
     * @param message           The message the alert should display
     * @param title             The title of the alert
     * @param buttonLabel       The label of the button
     * @param callbackContext   The callback context
     */
    public synchronized void alert(final String message, final String title, final String buttonLabel, final CallbackContext callbackContext) {
    	final CordovaInterface cordova = this.cordova;

        Runnable runnable = new Runnable() {
            public void run() {

                AlertDialog.Builder dlg = createDialog(cordova); // new AlertDialog.Builder(cordova.getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                dlg.setMessage(message);
                dlg.setTitle(title);
                dlg.setCancelable(false);
                dlg.setPositiveButton(buttonLabel,
                        new AlertDialog.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, 0));
                            }
                        });
                dlg.setOnCancelListener(new AlertDialog.OnCancelListener() {
                    public void onCancel(DialogInterface dialog)
                    {
                        dialog.dismiss();
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, 0));
                    }
                });

                changeTextDirection(dlg);
            };
        };
        this.cordova.getActivity().runOnUiThread(runnable);
    }

    /**
     * Builds and shows a native Android confirm dialog with given title, message, buttons.
     * This dialog only shows up to 3 buttons.  Any labels after that will be ignored.
     * The index of the button pressed will be returned to the JavaScript callback identified by callbackId.
     *
     * @param message           The message the dialog should display
     * @param title             The title of the dialog
     * @param buttonLabels      A comma separated list of button labels (Up to 3 buttons)
     * @param callbackContext   The callback context.
     */
    public synchronized void confirm(final String message, final String title, final JSONArray buttonLabels, final CallbackContext callbackContext) {
    	final CordovaInterface cordova = this.cordova;

        Runnable runnable = new Runnable() {
            public void run() {
                AlertDialog.Builder dlg = createDialog(cordova); // new AlertDialog.Builder(cordova.getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                dlg.setMessage(message);
                dlg.setTitle(title);
                dlg.setCancelable(false);

                // First button
                if (buttonLabels.length() > 0) {
                    try {
                        dlg.setNegativeButton(buttonLabels.getString(0),
                            new AlertDialog.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, 1));
                                }
                            });
                    } catch (JSONException e) { }
                }

                // Second button
                if (buttonLabels.length() > 1) {
                    try {
                        dlg.setNeutralButton(buttonLabels.getString(1),
                            new AlertDialog.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, 2));
                                }
                            });
                    } catch (JSONException e) { }
                }

                // Third button
                if (buttonLabels.length() > 2) {
                    try {
                        dlg.setPositiveButton(buttonLabels.getString(2),
                            new AlertDialog.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                  dialog.dismiss();
                                  callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, 3));
                                }
                            });
                    } catch (JSONException e) { }
                }
                dlg.setOnCancelListener(new AlertDialog.OnCancelListener() {
                    public void onCancel(DialogInterface dialog)
                    {
                        dialog.dismiss();
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, 0));
                    }
                });

                changeTextDirection(dlg);
            };
        };
        this.cordova.getActivity().runOnUiThread(runnable);
    }

    /**
     * Builds and shows a native Android prompt dialog with given title, message, buttons.
     * This dialog only shows up to 3 buttons.  Any labels after that will be ignored.
     * The following results are returned to the JavaScript callback identified by callbackId:
     *     buttonIndex			Index number of the button selected
     *     input1				The text entered in the prompt dialog box
     *
     * @param message           The message the dialog should display
     * @param title             The title of the dialog
     * @param buttonLabels      A comma separated list of button labels (Up to 3 buttons)
     * @param callbackContext   The callback context.
     */
    public synchronized void prompt(final String message, final String title, final JSONArray buttonLabels, final String defaultText,final String dialogType, final CallbackContext callbackContext) {
  	
        final CordovaInterface cordova = this.cordova;
       
        Runnable runnable = new Runnable() {
            public void run() {
                final EditText promptInput =  new EditText(cordova.getActivity());
                promptInput.setHint(defaultText);
                Log.d("DialogPlugin",dialogType);
                if(dialogType.equals(INPUT_SECURE)){
                	promptInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                AlertDialog.Builder dlg = createDialog(cordova); // new AlertDialog.Builder(cordova.getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                dlg.setMessage(message);
                dlg.setTitle(title);
                dlg.setCancelable(false);
                
                dlg.setView(promptInput);
                
                final JSONObject result = new JSONObject();
                
                // First button
                if (buttonLabels.length() > 0) {
                    try {
                        dlg.setNegativeButton(buttonLabels.getString(0),
                            new AlertDialog.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    try {
                                        result.put("buttonIndex",1);
                                        result.put("input1", promptInput.getText().toString().trim().length()==0 ? defaultText : promptInput.getText());											
                                    } catch (JSONException e) { e.printStackTrace(); }
                                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
                                }
                            });
                    } catch (JSONException e) { }
                }

                // Second button
                if (buttonLabels.length() > 1) {
                    try {
                        dlg.setNeutralButton(buttonLabels.getString(1),
                            new AlertDialog.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    try {
                                        result.put("buttonIndex",2);
                                        result.put("input1", promptInput.getText().toString().trim().length()==0 ? defaultText : promptInput.getText());
                                    } catch (JSONException e) { e.printStackTrace(); }
                                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
                                }
                            });
                    } catch (JSONException e) { }
                }

                // Third button
                if (buttonLabels.length() > 2) {
                    try {
                        dlg.setPositiveButton(buttonLabels.getString(2),
                            new AlertDialog.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    try {
                                        result.put("buttonIndex",3);
                                        result.put("input1", promptInput.getText().toString().trim().length()==0 ? defaultText : promptInput.getText());
                                    } catch (JSONException e) { e.printStackTrace(); }
                                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
                                }
                            });
                    } catch (JSONException e) { }
                }
                dlg.setOnCancelListener(new AlertDialog.OnCancelListener() {
                    public void onCancel(DialogInterface dialog){
                        dialog.dismiss();
                        try {
                            result.put("buttonIndex",0);
                            result.put("input1", promptInput.getText().toString().trim().length()==0 ? defaultText : promptInput.getText());
                        } catch (JSONException e) { e.printStackTrace(); }
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
                    }
                });

                changeTextDirection(dlg);
            };
        };
        this.cordova.getActivity().runOnUiThread(runnable);
    }

    /**
     * Builds and shows a native Android alert with given Strings
     * @param message           The message the alert should display
     * @param title             The title of the alert
     * @param buttonLabel       The label of the button
     * @param callbackContext   The callback context
     */
    public synchronized void list(final String title, final JSONArray data, final CallbackContext callbackContext) {
    	final CordovaInterface cordova = this.cordova;

    	final String[] options = new String[data.length()];
    	for(int i = 0;i < data.length();i++){
    		try {
				options[i] = data.getString(i);
			} catch (JSONException e) {
				e.printStackTrace();
			}
    	}
    	
        Runnable runnable = new Runnable() {
            public void run() {
            	final JSONObject result = new JSONObject();
                AlertDialog.Builder dlg = createDialog(cordova); // new AlertDialog.Builder(cordova.getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                dlg.setTitle(title);
                dlg.setCancelable(false);
                dlg.setItems(options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    	dialog.dismiss();
                    	try {
                    		result.put("buttonIndex", 1);
							result.put("selectedIndex", which);
						} catch (JSONException e) {
							e.printStackTrace();
						}
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
                    }
                });
                dlg.setNegativeButton("Cancel",
                        new AlertDialog.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                try {
									result.put("buttonIndex", 0);
								} catch (JSONException e) {
									e.printStackTrace();
								}
                                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
                            }
                        });
                dlg.setOnCancelListener(new AlertDialog.OnCancelListener() {
                    public void onCancel(DialogInterface dialog)
                    {
                        dialog.dismiss();
                        try {
							result.put("buttonIndex", 0);
						} catch (JSONException e) {
							e.printStackTrace();
						}
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
                    }
                });

                changeTextDirection(dlg);
            };
        };
        this.cordova.getActivity().runOnUiThread(runnable);
    }
    
    /**
     * 
     * @param message
     * @param title
     * @param buttonLabels
     * @param defaultTexts
     * @param callbackContext
     */
    public synchronized void login(final String title, final String message, final JSONArray buttonLabels, final JSONArray defaultTexts,final CallbackContext callbackContext) {
      	
        final CordovaInterface cordova = this.cordova;
       
        Runnable runnable = new Runnable() {
            public void run() {
            	LinearLayout layout = new LinearLayout(cordova.getActivity());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(10, 0, 10, 0);
                final EditText usernameInput = new EditText(cordova.getActivity());
                usernameInput.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_NORMAL);
                final EditText passwordInput = new EditText(cordova.getActivity());
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
                try {
                    usernameInput.setHint("ID");
                    usernameInput.setText(defaultTexts.getString(0));
                    passwordInput.setHint("PASSWORD");
                    passwordInput.setText(defaultTexts.getString(1));
                } catch (JSONException e1){}

                layout.addView(usernameInput, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
                layout.addView(passwordInput, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));

                AlertDialog.Builder dlg = createDialog(cordova); // new AlertDialog.Builder(cordova.getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                dlg.setMessage(message);
                dlg.setTitle(title);
                dlg.setCancelable(false);
                
                dlg.setView(layout);
                
                final JSONObject result = new JSONObject();
                
                    try {
                        dlg.setNegativeButton(buttonLabels.getString(0),
                            new AlertDialog.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    try {
                                        result.put("buttonIndex",1);
                                        result.put("input1", usernameInput.getText());		
                                        result.put("input2", passwordInput.getText());		
                                    } catch (JSONException e) { e.printStackTrace(); }
                                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
                                }
                            });
                    } catch (JSONException e) { }

                    try {
                        dlg.setPositiveButton(buttonLabels.getString(1),
                            new AlertDialog.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    try {
                                        result.put("buttonIndex",3);
                                    } catch (JSONException e) { e.printStackTrace(); }
                                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
                                }
                            });
                    } catch (JSONException e) { }
                    
                dlg.setOnCancelListener(new AlertDialog.OnCancelListener() {
                    public void onCancel(DialogInterface dialog){
                        dialog.dismiss();
                        try {
                            result.put("buttonIndex",0);
                        } catch (JSONException e) { e.printStackTrace(); }
                        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
                    }
                });

                changeTextDirection(dlg);
            };
        };
        this.cordova.getActivity().runOnUiThread(runnable);
    }
    
    
    @SuppressLint("NewApi")
    private AlertDialog.Builder createDialog(CordovaInterface cordova) {
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            return new AlertDialog.Builder(cordova.getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        } else {
            return new AlertDialog.Builder(cordova.getActivity());
        }
    }
    
    @SuppressLint("NewApi")
    private void changeTextDirection(Builder dlg){
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        dlg.create();
        AlertDialog dialog =  dlg.show();
        if (currentapiVersion >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            TextView messageview = (TextView)dialog.findViewById(android.R.id.message);
            if(messageview != null)
            messageview.setTextDirection(android.view.View.TEXT_DIRECTION_LOCALE);
        }
    }
}
