package com.example;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.Hashtable;

/**
 * Ứng dụng Swing: nhập chuỗi + key → mã hóa DES → xuất QR code
 * Quét lại QR để giải mã thành chuỗi gốc
 */
public class TicketQRApp extends JFrame {

    private JTextField inputField;
    private JTextField keyField;
    private JLabel qrLabel;
    private JTextArea outputArea;
    private JButton exportBtn; // nút xuất QR

    public TicketQRApp() {
        setTitle("🎟️ DES + QR Code Demo");
        setSize(600, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel nhập
        JPanel topPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        topPanel.add(new JLabel("Chuỗi cần mã hóa:"));
        inputField = new JTextField();
        topPanel.add(inputField);

        topPanel.add(new JLabel("Khóa (8 ký tự, vd: 12345678)"));
        keyField = new JTextField();
        topPanel.add(keyField);

        JButton genBtn = new JButton("Tạo QR Code");
        JButton scanBtn = new JButton("Giải mã từ QR");
        topPanel.add(genBtn);
        topPanel.add(scanBtn);
        add(topPanel, BorderLayout.NORTH);

        // Panel trung tâm hiển thị QR + nút xuất
        JPanel centerPanel = new JPanel(new BorderLayout());
        qrLabel = new JLabel("", SwingConstants.CENTER);
        qrLabel.setBorder(BorderFactory.createTitledBorder("Mã QR"));
        centerPanel.add(qrLabel, BorderLayout.CENTER);

        exportBtn = new JButton("Xuất QR");
        exportBtn.setEnabled(false); // chỉ bật khi có QR
        centerPanel.add(exportBtn, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // Panel output
        outputArea = new JTextArea(5, 20);
        outputArea.setEditable(false);
        outputArea.setBorder(BorderFactory.createTitledBorder("Kết quả"));
        add(new JScrollPane(outputArea), BorderLayout.SOUTH);

        // Xử lý nút bấm
        genBtn.addActionListener(e -> generateQR());
        scanBtn.addActionListener(e -> decodeQR());
        exportBtn.addActionListener(e -> exportQR());
    }

    /** Sinh QR code từ chuỗi đã mã hóa */
    private void generateQR() {
        try {
            String text = inputField.getText();
            String key = keyField.getText();
            if (text.isEmpty() || key.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nhập dữ liệu và key trước!");
                return;
            }
            if (key.length() != 8) {
                JOptionPane.showMessageDialog(this, "Key phải đúng 8 ký tự!");
                return;
            }

            String encrypted = DESExample.encryptDESBase64(text, key);

            int size = 250;
            Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = new MultiFormatWriter().encode(encrypted, BarcodeFormat.QR_CODE, size, size, hints);

            qrLabel.setIcon(new ImageIcon(MatrixToImageWriter.toBufferedImage(matrix)));
            outputArea.setText("Mã hóa (Base64):\n" + encrypted);

            exportBtn.setEnabled(true); // bật nút xuất khi đã có QR
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    /** Xuất QR ra file PNG */
    private void exportQR() {
        try {
            if (qrLabel.getIcon() == null) {
                JOptionPane.showMessageDialog(this, "Chưa có QR để xuất!");
                return;
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Chọn nơi lưu QR Code");
            chooser.setSelectedFile(new File("ticket_qr.png"));

            int result = chooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                String encrypted = outputArea.getText().split("\n")[1]; // lấy Base64 đã mã hóa

                int size = 250;
                Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
                hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
                BitMatrix matrix = new MultiFormatWriter().encode(encrypted, BarcodeFormat.QR_CODE, size, size, hints);

                Path path = file.toPath();
                MatrixToImageWriter.writeToPath(matrix, "PNG", path);

                JOptionPane.showMessageDialog(this, "Xuất QR thành công!\n" + file.getAbsolutePath());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi khi xuất: " + ex.getMessage());
        }
    }

    /** Đọc file QR code và giải mã */
    private void decodeQR() {
        try {
            File file = new File("ticket_qr.png");
            if (!file.exists()) {
                JOptionPane.showMessageDialog(this, "Chưa có file ticket_qr.png để quét!");
                return;
            }
            BufferedImage img = javax.imageio.ImageIO.read(file);
            LuminanceSource source = new BufferedImageLuminanceSource(img);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap);

            String key = keyField.getText();
            String decrypted = DESExample.decryptDESBase64(result.getText(), key);

            outputArea.setText("QR đọc được (Base64):\n" + result.getText() +
                               "\n\nGiải mã DES:\n" + decrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi giải mã: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TicketQRApp().setVisible(true));
    }
}