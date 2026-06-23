package com.memorycard.sync.ui;

/** Utilitário CLI para gerar icon.ico antes do jpackage. */
public class IconGenerator {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Uso: IconGenerator <caminho/icon.ico>");
            System.exit(1);
        }
        AppIcon.writeIcoFile(java.nio.file.Path.of(args[0]));
        System.out.println("Ícone gerado: " + args[0]);
    }
}
