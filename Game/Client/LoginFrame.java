package Game.Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginFrame extends JFrame {
    private Client client;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel statusLabel;

    private static final Logger LOGGER = Logger.getLogger(LoginFrame.class.getName());


    public LoginFrame(Client client) {
        this.client = client;
        setTitle("中国象棋 - 登录");
        setSize(380, 220); // 稍微调整大小
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // 图标 (可选)
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/img/rs.png")); // 使用一个棋子图标作为示例
            if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
                setIconImage(icon.getImage());
            }
        } catch (Exception e) {
            LOGGER.warning("无法加载窗口图标: " + e.getMessage());
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

        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));


        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        loginButton = new JButton("登录");
        registerButton = new JButton("注册");
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(Color.BLUE.darker());
        statusLabel.setPreferredSize(new Dimension(getWidth(), 25));


        add(statusLabel, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);


        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> client.openRegisterFrame()); // 调用Client的方法打开注册窗口

        getRootPane().setDefaultButton(loginButton);
    }

    private void handleLogin() {
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("<html><font color='red'>用户名或密码不能为空！</font></html>");
            return;
        }
        statusLabel.setText("正在连接服务器并登录...");
        loginButton.setEnabled(false);
        registerButton.setEnabled(false);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // 确保先连接，如果未连接
                if (client.connect()) { // connect()现在返回boolean
                    client.sendMessageToServer(new Info(Process.login, user, pass));
                    return true; // 表示尝试发送了登录请求
                }
                return false; // 连接失败
            }

            @Override
            protected void done() {
                try {
                    boolean attemptedLogin = get();
                    if (!attemptedLogin) { // 连接失败
                        // showConnectionError 已在 connect() 中通过JOptionPane显示，这里可以更新statusLabel
                        statusLabel.setText("<html><font color='red'>连接服务器失败，请检查服务器状态。</font></html>");
                        loginButton.setEnabled(true);
                        registerButton.setEnabled(true);
                    } else {
                        // 连接成功并发送了登录请求，等待服务器响应 (由Client.listenToServer处理)
                        // statusLabel.setText("已发送登录请求..."); // 可以不显示这个，直接等服务器结果
                    }
                } catch (Exception ex) {
                    statusLabel.setText("<html><font color='red'>登录处理异常: " + ex.getMessage()+"</font></html>");
                    LOGGER.log(Level.SEVERE, "登录处理异常", ex);
                    loginButton.setEnabled(true);
                    registerButton.setEnabled(true);
                }
            }
        }.execute();
    }

    public void showLoginError(String message) {
        statusLabel.setText("<html><font color='red'>登录失败: " + message + "</font></html>");
        loginButton.setEnabled(true);
        registerButton.setEnabled(true);
        passwordField.setText("");
        passwordField.requestFocus();
    }

    public void showConnectionError(String message) {
        statusLabel.setText("<html><font color='red'>" + message + "</font></html>");
        loginButton.setEnabled(true);
        registerButton.setEnabled(true);
    }

    public void showMessage(String message) {
        statusLabel.setText("<html><font color='blue'>" + message + "</font></html>");
    }
}
