package com.memorycard.sync.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public final class AppIcon {

    private AppIcon() {}

    public static Image create(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(99, 102, 241));
        g.fill(new RoundRectangle2D.Float(size * 0.08f, size * 0.08f, size * 0.84f, size * 0.84f, size * 0.18f, size * 0.18f));

        g.setColor(new Color(255, 255, 255, 220));
        g.fillRoundRect(Math.round(size * 0.22f), Math.round(size * 0.28f), Math.round(size * 0.56f), Math.round(size * 0.12f), size / 8, size / 8);
        g.fillRoundRect(Math.round(size * 0.22f), Math.round(size * 0.48f), Math.round(size * 0.56f), Math.round(size * 0.24f), size / 8, size / 8);

        g.setColor(new Color(79, 70, 229));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, size / 4)));
        FontMetrics fm = g.getFontMetrics();
        String text = "MC";
        g.drawString(text, (size - fm.stringWidth(text)) / 2, Math.round(size * 0.22f));

        g.dispose();
        return image;
    }

    /** Grava icon.ico para o instalador Windows (jpackage). */
    public static void writeIcoFile(java.nio.file.Path path) throws Exception {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.drawImage(create(256), 0, 0, null);
        g.dispose();

        java.nio.file.Files.createDirectories(path.getParent());
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(path.toFile())) {
            writeIco(out, image);
        }
    }

    private static void writeIco(java.io.OutputStream out, BufferedImage source) throws java.io.IOException {
        int[] sizes = {16, 32, 48, 256};
        java.util.List<byte[]> pngs = new java.util.ArrayList<>();
        for (int size : sizes) {
            BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, size, size, null);
            g.dispose();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(scaled, "PNG", baos);
            pngs.add(baos.toByteArray());
        }

        int count = pngs.size();
        int offset = 6 + count * 16;
        out.write(new byte[]{0, 0, 1, 0, (byte) count, 0});
        for (int i = 0; i < count; i++) {
            int size = sizes[i];
            byte[] png = pngs.get(i);
            out.write((byte) (size == 256 ? 0 : size));
            out.write((byte) (size == 256 ? 0 : size));
            out.write(0);
            out.write(0);
            out.write(1);
            out.write(0);
            out.write(32);
            out.write(0);
            writeIntLE(out, png.length);
            writeIntLE(out, offset);
            offset += png.length;
        }
        for (byte[] png : pngs) {
            out.write(png);
        }
    }

    private static void writeIntLE(java.io.OutputStream out, int value) throws java.io.IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }
}
