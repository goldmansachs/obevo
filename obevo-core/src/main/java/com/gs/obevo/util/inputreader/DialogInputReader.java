/**
 * Copyright 2017 Goldman Sachs.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.gs.obevo.util.inputreader;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.lang.StringUtils;

/**
 * Main user is {@link CredentialReader}.
 */
public class DialogInputReader implements UserInputReader {
    @Override
    public String readLine(String promptMessage) {
        final JTextField juf = new JTextField();
        JOptionPane juop = new JOptionPane(juf,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);
        JDialog userDialog = juop.createDialog(promptMessage);
        userDialog.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        juf.requestFocusInWindow();
                    }
                });
            }
        });
        userDialog.setVisible(true);
        int uresult = (Integer) juop.getValue();
        userDialog.dispose();
        String userName = null;
        if (uresult == JOptionPane.OK_OPTION) {
            userName = new String(juf.getText());
        }

        if (StringUtils.isEmpty(userName)) {
            return null;
        } else {
            return userName;
        }
    }

    @Override
    public String readPassword(String promptMessage) {
        final JPasswordField jpf = new JPasswordField();
        JOptionPane jop = new JOptionPane(jpf,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = jop.createDialog(promptMessage);
        dialog.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        jpf.requestFocusInWindow();
                    }
                });
            }
        });
        dialog.setVisible(true);
        int result = (Integer) jop.getValue();
        dialog.dispose();
        String password = null;
        if (result == JOptionPane.OK_OPTION) {
            password = new String(jpf.getPassword());
        }
        if (StringUtils.isEmpty(password)) {
            return null;
        } else {
            return password;
        }
    }
}
