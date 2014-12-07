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

#import "CDVNotification.h"
#import <Cordova/NSDictionary+Extensions.h>
#import <Cordova/NSArray+Comparisons.h>

#define DIALOG_TYPE_ALERT @"alert"
#define DIALOG_TYPE_PROMPT @"prompt"
#define DIALOG_TYPE_SECURE @"secure"
#define DIALOG_TYPE_LOGIN @"login"

@implementation CDVNotification

/*
 * showDialogWithMessage - Common method to instantiate the alert view for alert, confirm, and prompt notifications.
 * Parameters:
 *  message       The alert view message.
 *  title         The alert view title.
 *  buttons       The array of customized strings for the buttons.
 *  defaultText   The input text for the textbox (if textbox exists).
 *  callbackId    The commmand callback id.
 *  dialogType    The type of alert view [alert | prompt].
 */
- (void)showDialogWithMessage:(NSString*)message title:(NSString*)title buttons:(NSArray*)buttons defaultTexts:(NSArray*)defaultTexts callbackId:(NSString*)callbackId dialogType:(NSString*)dialogType
{
    CDVAlertView* alertView = [[CDVAlertView alloc]
                               initWithTitle:title
                               message:message
                               delegate:self
                               cancelButtonTitle:nil
                               otherButtonTitles:nil];
    
    alertView.callbackId = callbackId;
    
    NSUInteger count = [buttons count];
    
    for (int n = 0; n < count; n++) {
        [alertView addButtonWithTitle:[buttons objectAtIndex:n]];
    }
    
    if ([dialogType isEqualToString:DIALOG_TYPE_SECURE]) {
        alertView.alertViewStyle = UIAlertViewStyleSecureTextInput;
        UITextField* textField = [alertView textFieldAtIndex:0];
        textField.text = [defaultTexts objectAtIndex:0];
    }else if([dialogType isEqualToString:DIALOG_TYPE_PROMPT]){
        alertView.alertViewStyle = UIAlertViewStylePlainTextInput;
        UITextField* textField = [alertView textFieldAtIndex:0];
        textField.text = [defaultTexts objectAtIndex:0];
    }else if([dialogType isEqualToString:DIALOG_TYPE_LOGIN]){
        alertView.alertViewStyle = UIAlertViewStyleLoginAndPasswordInput;
        UITextField* idField = [alertView textFieldAtIndex:0];
        idField.text = [defaultTexts objectAtIndex:0];
        idField.placeholder = @"ID";
        UITextField* passField = [alertView textFieldAtIndex:1];
        passField.text = [defaultTexts objectAtIndex:1];
        passField.placeholder = @"PASSWORD";
    }
    
    [alertView show];
}

- (void)alert:(CDVInvokedUrlCommand*)command
{
    NSString* callbackId = command.callbackId;
    NSString* message = [command argumentAtIndex:0];
    NSString* title = [command argumentAtIndex:1];
    NSString* buttons = [command argumentAtIndex:2];
    
    [self showDialogWithMessage:message title:title buttons:@[buttons] defaultTexts:nil callbackId:callbackId dialogType:DIALOG_TYPE_ALERT];
}

- (void)confirm:(CDVInvokedUrlCommand*)command
{
    NSString* callbackId = command.callbackId;
    NSString* message = [command argumentAtIndex:0];
    NSString* title = [command argumentAtIndex:1];
    NSArray* buttons = [command argumentAtIndex:2];
    
    [self showDialogWithMessage:message title:title buttons:buttons defaultTexts:nil callbackId:callbackId dialogType:DIALOG_TYPE_ALERT];
}

- (void)prompt:(CDVInvokedUrlCommand*)command
{
    NSString* callbackId = command.callbackId;
    NSString* message = [command argumentAtIndex:0];
    NSString* title = [command argumentAtIndex:1];
    NSArray* buttons = [command argumentAtIndex:2];
    NSString* defaultText = [command argumentAtIndex:3];
    NSArray* defaultTexts = [NSArray arrayWithObjects:defaultText, nil];
    NSString* dialogType = [command argumentAtIndex:4];
    
    if ([dialogType isEqualToString:DIALOG_TYPE_SECURE]) {
        [self showDialogWithMessage:message title:title buttons:buttons defaultTexts:defaultTexts callbackId:callbackId dialogType:DIALOG_TYPE_SECURE];
    }else{
        [self showDialogWithMessage:message title:title buttons:buttons defaultTexts:defaultTexts callbackId:callbackId dialogType:DIALOG_TYPE_PROMPT];
    }
    
}

- (void)login:(CDVInvokedUrlCommand*)command
{
    NSString* callbackId = command.callbackId;
    NSString* message = [command argumentAtIndex:1];
    NSString* title = [command argumentAtIndex:0];
    NSArray* buttons = [command argumentAtIndex:2];
    NSArray* defaultTexts = [command argumentAtIndex:3];
    
    
    [self showDialogWithMessage:message title:title buttons:buttons defaultTexts:defaultTexts callbackId:callbackId dialogType:DIALOG_TYPE_LOGIN];
    
}

/**
 * Callback invoked when an alert dialog's buttons are clicked.
 */
- (void)alertView:(UIAlertView*)alertView clickedButtonAtIndex:(NSInteger)buttonIndex
{
    CDVAlertView* cdvAlertView = (CDVAlertView*)alertView;
    CDVPluginResult* result;
    
    // Determine what gets returned to JS based on the alert view type.
    if (alertView.alertViewStyle == UIAlertViewStyleDefault) {
        // For alert and confirm, return button index as int back to JS.
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:(int)(buttonIndex + 1)];
    }
    else if(alertView.alertViewStyle == UIAlertViewStyleLoginAndPasswordInput){
        // For prompt, return button index and input text back to JS.
        NSString* value0 = [[alertView textFieldAtIndex:0] text];
        NSString* value1 = [[alertView textFieldAtIndex:1] text];
        NSDictionary* info = @{
                               @"buttonIndex":@(buttonIndex + 1),
                               @"input1":(value0 ? value0 : [NSNull null]),
                               @"input2":(value1 ? value1 : [NSNull null])
                               };
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:info];
    }
    else {
        // For prompt, return button index and input text back to JS.
        NSString* value0 = [[alertView textFieldAtIndex:0] text];
        NSDictionary* info = @{
                               @"buttonIndex":@(buttonIndex + 1),
                               @"input1":(value0 ? value0 : [NSNull null])
                               };
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:info];
    }
    [self.commandDelegate sendPluginResult:result callbackId:cdvAlertView.callbackId];
}



@end

@implementation CDVAlertView

@synthesize callbackId;

@end
