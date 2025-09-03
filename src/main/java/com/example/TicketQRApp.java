package com.example;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.Hashtable;

public class TicketQRApp extends JFrame {

    private JTextField idField, nameField, passportField, phoneField, emailField, addressField, keyField;
    private JTextArea specialRequestArea, outputArea;
    private JLabel qrLabel;
    private JButton exportBtn;

    public TicketQRApp() {
        setTitle("🎟️ Quản lý Vé điện tử (DES + QR)");
        setSize(800, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel nhập form
        JPanel formPanel = new JPanel(new GridLayout(8, 2, 5, 5));
        formPanel.setBorder(BorderFactory.createTitledBorder("Thông tin vé"));

        formPanel.add(new JLabel("Mã vé:"));
        idField = new JTextField();
        formPanel.add(idField);

        formPanel.add(new JLabel("Tên KH:"));
        nameField = new JTextField();
        formPanel.add(nameField);

        formPanel.add(new JLabel("Passport/CCCD:"));
        passportField = new JTextField();
        formPanel.add(passportField);

        formPanel.add(new JLabel("Số điện thoại:"));
        phoneField = new JTextField();
        formPanel.add(phoneField);

        formPanel.add(new JLabel("Email:"));
        emailField = new JTextField();
        formPanel.add(emailField);

        formPanel.add(new JLabel("Địa chỉ:"));
        addressField = new JTextField();
        formPanel.add(addressField);

        formPanel.add(new JLabel("Yêu cầu đặc biệt:"));
        specialRequestArea = new JTextArea(3, 20);
        formPanel.add(new JScrollPane(specialRequestArea));

        formPanel.add(new JLabel("Key (8 ký tự):"));
        keyField = new JTextField();
        formPanel.add(keyField);

        add(formPanel, BorderLayout.NORTH);

        // Panel hiển thị QR
        JPanel centerPanel = new JPanel(new BorderLayout());
        qrLabel = new JLabel("", SwingConstants.CENTER);
        qrLabel.setBorder(BorderFactory.createTitledBorder("Mã QR"));
        centerPanel.add(qrLabel, BorderLayout.CENTER);

        exportBtn = new JButton("Xuất QR");
        exportBtn.setEnabled(false);
        centerPanel.add(exportBtn, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        // Panel nút chức năng
        JPanel buttonPanel = new JPanel();
        JButton genBtn = new JButton("Tạo QR Code");
        JButton scanBtn = new JButton("Giải mã từ QR");
        buttonPanel.add(genBtn);
        buttonPanel.add(scanBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        // Panel output
        outputArea = new JTextArea(6, 20);
        outputArea.setEditable(false);
        outputArea.setBorder(BorderFactory.createTitledBorder("Kết quả"));
        add(new JScrollPane(outputArea), BorderLayout.EAST);

        // Sự kiện nút
        genBtn.addActionListener(e -> generateQR());
        exportBtn.addActionListener(e -> exportQR());
        scanBtn.addActionListener(e -> openDecodeWindow());

        // ✅ Realtime validation với tooltip
        addRealtimeValidation();
    }

    /** Realtime validation với tooltip */
    /** Realtime validation với tooltip */
    private void addRealtimeValidation() {
        // Check phone
        phoneField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String phone = phoneField.getText().trim();
                if (!phone.matches("^(0\\d{9,10}|\\+84\\d{9,10})$")) {
                    phoneField.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                    phoneField.setToolTipText("SĐT phải 10 số, bắt đầu 0 hoặc +84");
                } else {
                    phoneField.setBorder(UIManager.getLookAndFeel().getDefaults().getBorder("TextField.border"));
                    phoneField.setToolTipText(null);
                }
            }
        });

        // Check email
        emailField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String email = emailField.getText().trim();
                if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                    emailField.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                    emailField.setToolTipText("Email không hợp lệ");
                } else {
                    emailField.setBorder(UIManager.getLookAndFeel().getDefaults().getBorder("TextField.border"));
                    emailField.setToolTipText(null);
                }
            }
        });

        // Check key
        keyField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String key = keyField.getText().trim();
                if (key.length() != 8) {
                    keyField.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                    keyField.setToolTipText("Key DES phải đúng 8 ký tự");
                } else {
                    keyField.setBorder(UIManager.getLookAndFeel().getDefaults().getBorder("TextField.border"));
                    keyField.setToolTipText(null);
                }
            }
        });

        // ✅ Check các trường bắt buộc khác (id, name, passport, address)
        JTextField[] requiredFields = { idField, nameField, passportField, addressField };
        for (JTextField field : requiredFields) {
            field.addKeyListener(new KeyAdapter() {
                public void keyReleased(KeyEvent e) {
                    if (field.getText().trim().isEmpty()) {
                        field.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
                        field.setToolTipText("Trường này không được để trống");
                    } else {
                        field.setBorder(UIManager.getLookAndFeel().getDefaults().getBorder("TextField.border"));
                        field.setToolTipText(null);
                    }
                }
            });
        }
    }

    /** Gom thông tin vé thành JSON string */
    private String buildTicketInfoJSON() {
        JSONObject obj = new JSONObject();
        obj.put("id", idField.getText());
        obj.put("name", nameField.getText());
        obj.put("passport", passportField.getText());
        obj.put("phone", phoneField.getText());
        obj.put("email", emailField.getText());
        obj.put("address", addressField.getText());
        obj.put("request", specialRequestArea.getText());
        return obj.toString();
    }

    /** Đổ dữ liệu JSON ra form */
    private void fillFormFromJSON(String json) {
        JSONObject obj = new JSONObject(json);
        idField.setText(obj.optString("id"));
        nameField.setText(obj.optString("name"));
        passportField.setText(obj.optString("passport"));
        phoneField.setText(obj.optString("phone"));
        emailField.setText(obj.optString("email"));
        addressField.setText(obj.optString("address"));
        specialRequestArea.setText(obj.optString("request"));
    }

    /** Sinh QR từ dữ liệu */
    private void generateQR() {
        try {
            String key = keyField.getText().trim();
            if (key.length() != 8) {
                JOptionPane.showMessageDialog(this, "Key DES phải đúng 8 ký tự!");
                return;
            }

            String ticketInfo = buildTicketInfoJSON();
            String encrypted = DESExample.encryptDESBase64(ticketInfo, key);

            int size = 300;
            Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = new MultiFormatWriter().encode(encrypted, BarcodeFormat.QR_CODE, size, size, hints);

            qrLabel.setIcon(new ImageIcon(MatrixToImageWriter.toBufferedImage(matrix)));
            outputArea.setText("Chuỗi đã mã hóa (Base64):\n" + encrypted);

            exportBtn.setEnabled(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    /** Xuất QR ra file PNG */
    private void exportQR() {
        try {
            if (qrLabel.getIcon() == null)
                return;
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Chọn nơi lưu QR");
            chooser.setSelectedFile(new File("ticket_qr.png"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                String encrypted = outputArea.getText().split("\n", 2)[1];

                int size = 300;
                Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
                hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                BitMatrix matrix = new MultiFormatWriter().encode(encrypted, BarcodeFormat.QR_CODE, size, size, hints);

                Path path = file.toPath();
                MatrixToImageWriter.writeToPath(matrix, "PNG", path);

                JOptionPane.showMessageDialog(this, "Đã lưu QR tại:\n" + file.getAbsolutePath());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Cửa sổ decode QR */
    private void openDecodeWindow() {
        JDialog dialog = new JDialog(this, "Giải mã từ QR", true);
        dialog.setSize(500, 400);
        dialog.setLayout(new BorderLayout());

        JButton browseBtn = new JButton("Browse QR Image");
        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setBorder(BorderFactory.createTitledBorder("Thông tin giải mã"));

        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try {
                    BufferedImage img = javax.imageio.ImageIO.read(file);
                    LuminanceSource source = new BufferedImageLuminanceSource(img);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                    Result result = new MultiFormatReader().decode(bitmap);

                    // hỏi key để giải mã
                    String key = JOptionPane.showInputDialog(this, "Nhập key (8 ký tự) để giải mã:");
                    if (key == null || key.length() != 8) {
                        JOptionPane.showMessageDialog(this, "Key DES phải đúng 8 ký tự!");
                        return;
                    }

                    String decrypted = DESExample.decryptDESBase64(result.getText(), key);

                    // đổ ngược dữ liệu vào form
                    fillFormFromJSON(decrypted);

                    resultArea.setText("JSON giải mã:\n" + decrypted);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Lỗi giải mã: " + ex.getMessage());
                }
            }
        });

        dialog.add(browseBtn, BorderLayout.NORTH);
        dialog.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TicketQRApp().setVisible(true));
    }
}