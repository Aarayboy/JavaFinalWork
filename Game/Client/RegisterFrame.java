package Game.Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RegisterFrame extends JFrame {
    private Client client;
    private LoginFrame loginFrameInstance; // 用于注册后返回

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmField;
    private JButton registerButton;
    private JButton backButton;
    private JLabel statusLabel;

    private static final Logger LOGGER = Logger.getLogger(RegisterFrame.class.getName());


    public RegisterFrame(Client client, LoginFrame loginFrameInstance) {
        this.client = client;
        this.loginFrameInstance = loginFrameInstance;

        setTitle("中国象棋 - 用户注册");
        setSize(400, 280);
        setLocationRelativeTo(null); // 居中
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // 由backButton或windowClosing处理关闭
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                goBackToLogin(false);
            }
        });
        setLayout(new BorderLayout(10,10));

        // 图标
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/img/rs.png"));
            if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
                setIconImage(icon.getImage());
            }
        } catch (Exception e) {
            LOGGER.warning("无法加载注册窗口图标: " + e.getMessage());
        }

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("用户名:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        usernameField = new JTextField(15);
        formPanel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        formPanel.add(new JLabel("密码:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        passwordField = new JPasswordField(15);
        formPanel.add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        formPanel.add(new JLabel("确认密码:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        confirmField = new JPasswordField(15);
        formPanel.add(confirmField, gbc);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));


        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        registerButton = new JButton("注册");
        backButton = new JButton("返回登录");
        buttonPanel.add(registerButton);
        buttonPanel.add(backButton);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(Color.BLUE.darker());
        statusLabel.setPreferredSize(new Dimension(getWidth(), 25));

        add(statusLabel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        registerButton.addActionListener(e -> handleRegister());
        backButton.addActionListener(e -> goBackToLogin(false));
        getRootPane().setDefaultButton(registerButton);
    }

    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("<html><font color='red'>用户名或密码不能为空！</font></html>");
            return;
        }
        if (username.length() < 3 || password.length() < 3) {
            statusLabel.setText("<html><font color='red'>用户名和密码长度至少为3位。</font></html>");
            return;
        }
        if (!password.equals(confirm)) {
            statusLabel.setText("<html><font color='red'>两次输入的密码不一致！</font></html>");
            return;
        }

        statusLabel.setText("正在发送注册请求...");
        registerButton.setEnabled(false);
        backButton.setEnabled(false);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                if (client.connect()) { // 确保连接，如果尚未连接
                    client.sendMessageToServer(new Info(Process.register, username, password));
                    return true;
                }
                return false; // 连接失败
            }

            @Override
            protected void done() {
                try {
                    boolean attemptedRegister = get();
                    if (!attemptedRegister) {
                        showError("连接服务器失败，请重试。");
                        // registerButton.setEnabled(true); // showError会处理
                        // backButton.setEnabled(true);
                    } else {
                        // 等待服务器响应 (由Client.listenToServer处理)
                        // statusLabel.setText("已发送注册请求...");
                    }
                } catch (Exception ex) {
                    showError("注册处理异常: " + ex.getMessage());
                    LOGGER.log(Level.SEVERE, "注册处理异常", ex);
                }
            }
        }.execute();
    }

    public void showRegisterSuccess(String message) {
        statusLabel.setText("<html><font color='green'>" + message + "</font></html>");
        JOptionPane.showMessageDialog(this, message, "注册成功", JOptionPane.INFORMATION_MESSAGE);
        registerButton.setEnabled(false); // 注册成功后禁用注册按钮
        backButton.setText("去登录"); // 修改返回按钮文本
        backButton.setEnabled(true);
        // 用户可以点击“去登录”按钮返回
    }

    public void showRegisterError(String message) {
        statusLabel.setText("<html><font color='red'>注册失败: " + message + "</font></html>");
        registerButton.setEnabled(true);
        backButton.setEnabled(true);
        passwordField.setText("");
        confirmField.setText("");
        usernameField.requestFocus();
    }

    public void showError(String message) { // 通用错误显示
        statusLabel.setText("<html><font color='red'>" + message + "</font></html>");
        registerButton.setEnabled(true);
        backButton.setEnabled(true);
    }

    public void showMessage(String message) { // 通用消息显示
        statusLabel.setText("<html><font color='blue'>" + message + "</font></html>");
    }

    private void goBackToLogin(boolean fromRegisterSuccess) {
        // Client类现在有一个方法来处理返回登录界面
        client.backToLoginFrame(fromRegisterSuccess);
        // dispose(); // Client.backToLoginFrame 内部会处理旧RegisterFrame的dispose
    }
}