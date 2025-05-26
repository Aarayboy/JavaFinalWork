package Game.Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClockPanel extends JPanel implements ActionListener {

    private int gameTotalSecondsInit;
    private int turnTotalSecondsInit;

    private int gameSecondsLeft;
    private int turnSecondsLeft;

    private final JLabel gameLabel = new JLabel();
    private final JLabel turnLabel = new JLabel();
    private final Timer timer = new Timer(1000, this);

    public ClockPanel(int gameTotalSeconds, int turnTotalSeconds) {
        reinitializeTimers(gameTotalSeconds, turnTotalSeconds); // 使用新方法初始化
        setOpaque(false);
        setLayout(new GridLayout(2, 1));

        gameLabel.setForeground(new Color(220, 220, 220)); // 更亮的白色
        turnLabel.setForeground(new Color(220, 220, 200)); // 略带黄色
        gameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        turnLabel.setHorizontalAlignment(SwingConstants.CENTER);
        Font clockFont = new Font("Arial", Font.BOLD, 14); // 更改字体和大小
        gameLabel.setFont(clockFont);
        turnLabel.setFont(clockFont);

        add(gameLabel);
        add(turnLabel);

        // timer.start(); // 不再自动开始，由GameGUI控制
    }

    public void reinitializeTimers(int gameTotalSeconds, int turnTotalSeconds) {
        this.gameTotalSecondsInit = gameTotalSeconds;
        this.turnTotalSecondsInit = turnTotalSeconds;
        this.gameSecondsLeft = gameTotalSeconds;
        this.turnSecondsLeft = turnTotalSeconds;
        refreshLabels();
    }


    public void resetTurn() {
        turnSecondsLeft = turnTotalSecondsInit;
        refreshLabels();
    }

    public void pause() {
        if (timer.isRunning()) timer.stop();
    }

    public void resume() {
        if (!timer.isRunning() && (gameSecondsLeft > 0 || turnSecondsLeft > 0) ) timer.start();
        refreshLabels();
    }

    public void stop() {
        timer.stop();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        boolean changed = false;
        if (gameSecondsLeft > 0) {
            gameSecondsLeft--;
            changed = true;
        }
        if (turnSecondsLeft > 0) {
            turnSecondsLeft--;
            changed = true;
        }

        if (changed) {
            refreshLabels();
        }

        if (gameSecondsLeft <= 0 && turnSecondsLeft <= 0 && timer.isRunning()) {
            // timer.stop(); // 时间都到了可以停止，或者由外部逻辑判断超时
            // 实际超时判负应由服务器处理，客户端仅显示
        }
    }

    private void refreshLabels() {
        gameLabel.setText("局时 " + format(gameSecondsLeft));
        turnLabel.setText("步时 " + format(turnSecondsLeft));
    }

    private static String format(int sec) {
        if (sec < 0) sec = 0; // 防止显示负数
        int m = sec / 60;
        int s = sec % 60;
        return String.format("%02d:%02d", m, s);
    }
}
