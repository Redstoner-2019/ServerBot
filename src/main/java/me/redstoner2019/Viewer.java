package me.redstoner2019;

import me.redstoner2019.utils.CompressionUtility;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Viewer {
    public static void main(String[] args) throws IOException {
        JFrame frame = new JFrame("Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1920,1080);
        frame.setLocationRelativeTo(null);

        File file = new File("nudes");

        JPanel panel = new JPanel();
        panel.setLayout(null);
        frame.setContentPane(panel);

        DefaultListModel<String> listModel = new DefaultListModel<>();

        JList<String> list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(list);
        panel.add(scrollPane);
        scrollPane.setBounds(20, 0, 800, 1080);

        JLabel label = new JLabel();
        label.setBounds(840,0,1080,1080);
        panel.add(label);

        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                try {
                    label.setIcon(new ImageIcon(resize(ImageIO.read(CompressionUtility.decompressFileAsStream(new File("nudes/" +  list.getSelectedValue()))),1080,1080)));
                } catch (Exception ex) {

                }
            }
        });

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    listModel.removeAllElements();
                    for(String s : file.list()){
                        listModel.addElement(s);
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        t.start();

        frame.setVisible(true);
    }

    public static BufferedImage resize(BufferedImage originalImage, int maxWidth, int maxHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double scale = Math.min(widthRatio, heightRatio); // scale UP or DOWN, depending

        int newWidth = (int) Math.round(originalWidth * scale);
        int newHeight = (int) Math.round(originalHeight * scale);

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
        g2d.dispose();

        return resizedImage;
    }
}
