import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class CalculatorUI extends JFrame {

    private JTextField display;
    private JTextArea historyArea;
    private String expression = "";

    private JSplitPane splitPane;

    private static final int GAP = 10;
    private static final int BTN_HEIGHT = 45;
    private static final int CALC_WIDTH = 360;

    public CalculatorUI() {

        setTitle("Advanced Calculator");
        setSize(420, 580);
        setMinimumSize(new Dimension(420, 580));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ===== Display =====
        display = new JTextField("0");
        display.setFont(new Font("Segoe UI", Font.BOLD, 28));
        display.setHorizontalAlignment(JTextField.RIGHT);
        display.setEditable(false);
        display.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        add(display, BorderLayout.NORTH);

        JPanel calcPanel = createCalculatorPanel();
        JPanel historyPanel = createHistoryPanel();

        // ===== FIXED LEFT CALCULATOR WRAPPER =====
        JPanel calcWrapper = new JPanel(new BorderLayout());
        calcWrapper.setPreferredSize(new Dimension(CALC_WIDTH, 0));
        calcWrapper.add(calcPanel, BorderLayout.CENTER); // 🔥 IMPORTANT

        splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                calcWrapper,
                new JPanel()
        );
        splitPane.setDividerSize(0);
        splitPane.setResizeWeight(0.70);
        add(splitPane, BorderLayout.CENTER);

        // = Fullscreen 70 / 30 =
        addWindowStateListener(e -> {
            boolean max =
                    (e.getNewState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
            if (max) {
                splitPane.setRightComponent(historyPanel);
                splitPane.setDividerSize(4);
                splitPane.setDividerLocation((int) (getWidth() * 0.70));
                historyArea.setText(HistoryManager.load());
            } else {
                splitPane.setRightComponent(new JPanel());
                splitPane.setDividerSize(0);
            }
        });

        setupKeyboard();
        setVisible(true);
    }

    // ================= Calculator Panel =================
    private JPanel createCalculatorPanel() {

        JPanel grid = new JPanel(new GridLayout(5, 4, GAP, GAP));
        grid.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));
        grid.setPreferredSize(new Dimension(CALC_WIDTH, 0));

        String[] buttons = {
                "(", ")", "%", "C",
                "7", "8", "9", "/",
                "4", "5", "6", "*",
                "1", "2", "3", "-",
                "0", ".", "⌫", "+"
        };

        for (String t : buttons) {
            JButton b = createButton(t);
            b.addActionListener(e -> handleInput(t));
            grid.add(b);
        }

        // ===== Bottom Row =====
        JPanel bottom = new JPanel(new GridBagLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(0, GAP, GAP, GAP));
        bottom.setPreferredSize(new Dimension(CALC_WIDTH, BTN_HEIGHT));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridy = 0;
        c.weighty = 1;

        JButton ch = createButton("CH");
        ch.addActionListener(e -> {
            int opt = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to clear history?",
                    "Clear History",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (opt == JOptionPane.YES_OPTION) {
                HistoryManager.clear();
                if (historyArea != null) historyArea.setText("");
            }
        });

        c.gridx = 0;
        c.weightx = 0.3;
        c.insets = new Insets(0, 0, 0, GAP);
        bottom.add(ch, c);

        JButton eq = createButton("=");
        eq.setFont(new Font("Segoe UI", Font.BOLD, 18));
        eq.addActionListener(e -> evaluate());

        c.gridx = 1;
        c.weightx = 0.7;
        c.insets = new Insets(0, 0, 0, 0);
        bottom.add(eq, c);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(grid, BorderLayout.CENTER);
        wrapper.add(bottom, BorderLayout.SOUTH);

        return wrapper;
    }

    // ================= History Panel =================
    private JPanel createHistoryPanel() {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(280, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));

        JLabel title = new JLabel("History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Consolas", Font.PLAIN, 13));

        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(historyArea), BorderLayout.CENTER);
        return panel;
    }

    // ================= Button Factory =================
    private JButton createButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(0, BTN_HEIGHT));
        return b;
    }

    // ================= Input =================
    private void handleInput(String cmd) {

        if (cmd.equals("C")) {
            expression = "";
            display.setText("0");
            return;
        }

        if (cmd.equals("⌫")) {
            if (!expression.isEmpty()) {
                expression = expression.substring(0, expression.length() - 1);
                display.setText(expression.isEmpty() ? "0" : expression);
            }
            return;
        }

        expression += cmd;
        display.setText(expression);
    }

    // ================= Evaluation =================
    private void evaluate() {
        try {
            String safe = normalizeExpression(expression);
            double result = evaluateExpression(safe);

            String out = expression + " = " + result;
            display.setText(out);

            HistoryManager.save(out);
            if (historyArea != null)
                historyArea.setText(HistoryManager.load());

            expression = "";
        } catch (Exception e) {
            display.setText("Error");
            expression = "";
        }
    }

    // ================= Implicit Multiplication =================
    private String normalizeExpression(String exp) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < exp.length(); i++) {
            char curr = exp.charAt(i);
            if (i > 0) {
                char prev = exp.charAt(i - 1);
                if (Character.isDigit(prev) && curr == '(') sb.append('*');
                if (prev == ')' && Character.isDigit(curr)) sb.append('*');
                if (prev == ')' && curr == '(') sb.append('*');
            }
            sb.append(curr);
        }
        return sb.toString();
    }

    // ================= Pure Java Evaluator =================
    private double evaluateExpression(String exp) {

        Stack<Double> values = new Stack<>();
        Stack<Character> ops = new Stack<>();

        for (int i = 0; i < exp.length(); i++) {

            char c = exp.charAt(i);

            if (Character.isDigit(c) || c == '.') {
                StringBuilder sb = new StringBuilder();
                while (i < exp.length() &&
                        (Character.isDigit(exp.charAt(i)) || exp.charAt(i) == '.')) {
                    sb.append(exp.charAt(i++));
                }
                i--;
                values.push(Double.parseDouble(sb.toString()));
            }
            else if (c == '(') ops.push(c);

            else if (c == ')') {
                while (ops.peek() != '(')
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()));
                ops.pop();
            }
            else if ("+-*/%".indexOf(c) >= 0) {
                while (!ops.empty() && precedence(c) <= precedence(ops.peek()))
                    values.push(applyOp(ops.pop(), values.pop(), values.pop()));
                ops.push(c);
            }
        }

        while (!ops.empty())
            values.push(applyOp(ops.pop(), values.pop(), values.pop()));

        return values.pop();
    }

    private int precedence(char op) {
        if (op == '+' || op == '-') return 1;
        if (op == '*' || op == '/' || op == '%') return 2;
        return 0;
    }

    private double applyOp(char op, double b, double a) {
        switch (op) {
            case '+': return a + b;
            case '-': return a - b;
            case '*': return a * b;
            case '/': return a / b;
            case '%': return a % b;
        }
        return 0;
    }

    // ================= Keyboard =================
    private void setupKeyboard() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(e -> {
                    if (e.getID() != KeyEvent.KEY_PRESSED) return false;
                    char c = e.getKeyChar();
                    if ("0123456789.+-*/()%".indexOf(c) >= 0)
                        handleInput(String.valueOf(c));
                    else if (e.getKeyCode() == KeyEvent.VK_ENTER)
                        evaluate();
                    else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE)
                        handleInput("⌫");
                    else if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                        handleInput("C");
                    return false;
                });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CalculatorUI::new);
    }
}
