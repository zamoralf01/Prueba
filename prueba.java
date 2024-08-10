/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.umg.album;

import java.awt.BorderLayout;
import java.awt.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;

import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Zamora
 */
public class prueba extends javax.swing.JFrame {

    private JFileChooser fileChooser;
    DefaultListModel<String> modeloLista;
    JList<String> listaImagenes;
    ArrayList<String> rutasImagenes;
    private Timer timer;
    private File archivoOriginal;

    /**
     * Creates new form prueba
     */
    public prueba() {
        super("Album de Fotos");
        initComponents();

        timer = new Timer();
        fileChooser = new JFileChooser();
    }

    private boolean loopInfinito = false;
    private boolean usoManual = false;
    private int indiceImagen = 0;
    private volatile boolean pausado = false;
    private Thread albumThread;
    private boolean albumCargado = false;
    private boolean albumModificado = false;

    private void abrirArchivo() {
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        int resultado = fileChooser.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            archivoOriginal = archivo;
            leerArchivo(archivo);
            albumCargado = true;
            playAlbum();
        }
    }

    private void agregarImagen() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home") + "/Downloads"));
        int resultado = fileChooser.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            String ruta = archivo.getAbsolutePath();
            rutasImagenes.add(ruta);
            albumModificado = true;
            modeloLista.addElement(ruta.substring(ruta.lastIndexOf("/") + 1));
        }
    }

    private void guardarAlbum() {
        if (archivoOriginal != null && albumModificado) {
            guardarAlbum(archivoOriginal);
        } else {
            guardarAlbumComo();
        }
    }

    private void guardarAlbumComo() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int resultado = fileChooser.showSaveDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            guardarAlbum(archivo);
        }
    }

    private void guardarAlbum(File archivo) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element album = doc.createElement("album");
            doc.appendChild(album);

            for (String ruta : rutasImagenes) {
                Element imagen = doc.createElement("imagen");
                album.appendChild(imagen);
                imagen.setAttribute("ruta", ruta);
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(archivo);
            transformer.transform(source, result);

            JOptionPane.showMessageDialog(this, "Álbum guardado con éxito en " + archivo.getName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al guardar el álbum");
        }
    }

    private void playAlbum() {
        if (albumCargado) {
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
            timer = new Timer();
            indiceImagen = 0;
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (!pausado) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                mostrarImagenGrande(rutasImagenes.get(indiceImagen));
                                indiceImagen++;
                                if (indiceImagen >= rutasImagenes.size()) {
                                    if (loopInfinito) {
                                        indiceImagen = 0;
                                    } else {
                                        timer.cancel();
                                        timer = null;
                                        JOptionPane.showMessageDialog(prueba.this, "Álbum finalizado");
                                    }
                                }
                            }
                        });
                    }
                }
            }, 0, 5000);
        } else {
            JOptionPane.showMessageDialog(this, "No hay álbum cargado");
        }
    }

    private void pauseAlbum() {
        pausado = true;
        if (albumThread != null) {
            albumThread.interrupt();
            albumThread = null;
        }
    }

    private void reanudarAlbum() {
        pausado = false;
    }

    private void leerArchivo(File archivo) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(archivo);
            NodeList imagenes = doc.getElementsByTagName("imagen");
            rutasImagenes.clear();
            modeloLista.clear();
            for (int i = 0; i < imagenes.getLength(); i++) {
                Node imagen = imagenes.item(i);
                if (imagen.getNodeType() == Node.ELEMENT_NODE) {
                    Element elemento = (Element) imagen;
                    String ruta = elemento.getAttribute("ruta");
                    if (rutasImagenes == null) {
                        rutasImagenes = new ArrayList<>();
                    }
                    rutasImagenes.add(ruta);
                    modeloLista.addElement(ruta.substring(ruta.lastIndexOf("/") + 1));
                }
            }
            if (rutasImagenes != null && !rutasImagenes.isEmpty()) {
                mostrarImagenGrande(rutasImagenes.get(0));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al leer el archivo");
        }
    }

    private void mostrarImagenGrande(String ruta) {
        ImageIcon icono = new ImageIcon(ruta);
        Image imagen = icono.getImage();
        int ancho = 600;
        int alto = 400;
        imagen = imagen.getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
        icono = new ImageIcon(imagen);
        labelImagenGrande.setIcon(icono);
        panelImagenGrande.revalidate();
        panelImagenGrande.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        botonPause = new javax.swing.JButton();
        botonPlay = new javax.swing.JButton();
        botonDetener = new javax.swing.JButton();
        botonUsoManual = new javax.swing.JButton();
        botonLoop = new javax.swing.JButton();
        botonAtras = new javax.swing.JButton();
        botonSiguiente = new javax.swing.JButton();
        botonIrPrimera = new javax.swing.JButton();
        botonIrUltima = new javax.swing.JButton();
        panelImagenGrande = new javax.swing.JPanel();
        labelImagenGrande = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        panelLista = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jList = new javax.swing.JList<>();
        javax.swing.JToolBar jToolBar = new javax.swing.JToolBar();
        botonAbrir = new javax.swing.JButton();
        botonAgregarImagen = new javax.swing.JButton();
        botonGuardarAlbum = new javax.swing.JButton();
        botonGuardarAlbumComo = new javax.swing.JButton();
        botonClose = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jPanel2.setBackground(new java.awt.Color(153, 153, 255));

        botonPause.setText("Pausa");
        botonPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonPauseActionPerformed(evt);
            }
        });

        botonPlay.setText("Play");
        botonPlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonPlayActionPerformed(evt);
            }
        });

        botonDetener.setText("Detener");
        botonDetener.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonDetenerActionPerformed(evt);
            }
        });

        botonUsoManual.setText("Uso Manual");
        botonUsoManual.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonUsoManualActionPerformed(evt);
            }
        });

        botonLoop.setText("Loop Infinito");
        botonLoop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonLoopActionPerformed(evt);
            }
        });

        botonAtras.setText("Anterior");
        botonAtras.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonAtrasActionPerformed(evt);
            }
        });

        botonSiguiente.setText("Siguiente");
        botonSiguiente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonSiguienteActionPerformed(evt);
            }
        });

        botonIrPrimera.setText("Ir Primera");
        botonIrPrimera.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonIrPrimeraActionPerformed(evt);
            }
        });

        botonIrUltima.setText("Ir Ultima");
        botonIrUltima.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonIrUltimaActionPerformed(evt);
            }
        });

        labelImagenGrande = new javax.swing.JLabel();
        panelImagenGrande.setBackground(new java.awt.Color(153, 204, 255));
        panelImagenGrande.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0), 2));
        panelImagenGrande.setPreferredSize(new java.awt.Dimension(600, 400));

        labelImagenGrande.setText("Prueba");

        panelImagenGrande.add(labelImagenGrande, BorderLayout.CENTER);

        javax.swing.GroupLayout panelImagenGrandeLayout = new javax.swing.GroupLayout(panelImagenGrande);
        panelImagenGrande.setLayout(panelImagenGrandeLayout);
        panelImagenGrandeLayout.setHorizontalGroup(
            panelImagenGrandeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 596, Short.MAX_VALUE)
            .addGroup(panelImagenGrandeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(panelImagenGrandeLayout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(labelImagenGrande)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );
        panelImagenGrandeLayout.setVerticalGroup(
            panelImagenGrandeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 343, Short.MAX_VALUE)
            .addGroup(panelImagenGrandeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(panelImagenGrandeLayout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(labelImagenGrande)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(61, 61, 61)
                .addComponent(panelImagenGrande, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(54, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(botonLoop)
                        .addGap(18, 18, 18)
                        .addComponent(botonPlay)
                        .addGap(18, 18, 18)
                        .addComponent(botonPause)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(botonDetener)
                        .addGap(18, 18, 18)
                        .addComponent(botonUsoManual))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(64, 64, 64)
                        .addComponent(botonIrPrimera)
                        .addGap(18, 18, 18)
                        .addComponent(botonAtras)
                        .addGap(18, 18, 18)
                        .addComponent(botonSiguiente)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(botonIrUltima)))
                .addGap(110, 110, 110))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(panelImagenGrande, javax.swing.GroupLayout.DEFAULT_SIZE, 347, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(botonPlay)
                    .addComponent(botonPause)
                    .addComponent(botonDetener)
                    .addComponent(botonUsoManual)
                    .addComponent(botonLoop))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(botonAtras)
                    .addComponent(botonSiguiente)
                    .addComponent(botonIrPrimera)
                    .addComponent(botonIrUltima))
                .addGap(38, 38, 38))
        );

        panelImagenGrande.add(labelImagenGrande, BorderLayout.CENTER);

        panelLista.setBackground(new java.awt.Color(204, 255, 204));
        panelLista.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                panelListaMouseClicked(evt);
            }
        });

        jList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jListMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jList);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 196, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 458, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout panelListaLayout = new javax.swing.GroupLayout(panelLista);
        panelLista.setLayout(panelListaLayout);
        panelListaLayout.setHorizontalGroup(
            panelListaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        panelListaLayout.setVerticalGroup(
            panelListaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelListaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(21, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(panelLista, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(panelLista, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 21, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        jToolBar.setBackground(new java.awt.Color(255, 255, 255));
        jToolBar.setFloatable(true);
        jToolBar.setRollover(true);

        botonAbrir.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.toolbar.selectedBackground"));
        botonAbrir.setForeground(new java.awt.Color(0, 0, 0));
        botonAbrir.setText("Abrir Archivo");
        botonAbrir.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        botonAbrir.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        botonAbrir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonAbrirActionPerformed(evt);
            }
        });
        jToolBar.add(botonAbrir);

        botonAgregarImagen.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.toolbar.selectedBackground"));
        botonAgregarImagen.setForeground(new java.awt.Color(0, 0, 0));
        botonAgregarImagen.setText("Agregar Fotos");
        botonAgregarImagen.setFocusable(false);
        botonAgregarImagen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        botonAgregarImagen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        botonAgregarImagen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonAgregarImagenActionPerformed(evt);
            }
        });
        jToolBar.add(botonAgregarImagen);

        botonGuardarAlbum.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.toolbar.selectedBackground"));
        botonGuardarAlbum.setForeground(new java.awt.Color(0, 0, 0));
        botonGuardarAlbum.setText("Guardar Album Existente");
        botonGuardarAlbum.setFocusable(false);
        botonGuardarAlbum.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        botonGuardarAlbum.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        botonGuardarAlbum.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonGuardarAlbumActionPerformed(evt);
            }
        });
        jToolBar.add(botonGuardarAlbum);

        botonGuardarAlbumComo.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.toolbar.selectedBackground"));
        botonGuardarAlbumComo.setForeground(new java.awt.Color(0, 0, 0));
        botonGuardarAlbumComo.setText("Guardar Nuevo Album");
        botonGuardarAlbumComo.setFocusable(false);
        botonGuardarAlbumComo.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        botonGuardarAlbumComo.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        botonGuardarAlbumComo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonGuardarAlbumComoActionPerformed(evt);
            }
        });
        jToolBar.add(botonGuardarAlbumComo);

        botonClose.setBackground(javax.swing.UIManager.getDefaults().getColor("Button.toolbar.selectedBackground"));
        botonClose.setForeground(new java.awt.Color(0, 0, 0));
        botonClose.setText("Cerrar Album Actual");
        botonClose.setFocusable(false);
        botonClose.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        botonClose.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        botonClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                botonCloseActionPerformed(evt);
            }
        });
        jToolBar.add(botonClose);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 485, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void botonDetenerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonDetenerActionPerformed
        if (timer != null) {
            timer.cancel();
            timer = null;
            indiceImagen = 0;
            mostrarImagenGrande(rutasImagenes.get(indiceImagen));
            pausado = false;
            botonPlay.setEnabled(true);
            botonPause.setEnabled(true);
            botonAtras.setEnabled(false);
            botonSiguiente.setEnabled(false);
            botonIrPrimera.setEnabled(false);
            botonIrUltima.setEnabled(false);
        }
    }//GEN-LAST:event_botonDetenerActionPerformed

    private void botonPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonPlayActionPerformed
        if (timer == null) {
            timer = new Timer();
            indiceImagen = 0;
            playAlbum();
        }
    }//GEN-LAST:event_botonPlayActionPerformed

    private void botonIrPrimeraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonIrPrimeraActionPerformed
        indiceImagen = 0;
        mostrarImagenGrande(rutasImagenes.get(indiceImagen));
    }//GEN-LAST:event_botonIrPrimeraActionPerformed

    private void botonAtrasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonAtrasActionPerformed
        if (indiceImagen > 0) {
            indiceImagen--;
            mostrarImagenGrande(rutasImagenes.get(indiceImagen));
        }
    }//GEN-LAST:event_botonAtrasActionPerformed

    private void botonSiguienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonSiguienteActionPerformed
        if (indiceImagen < rutasImagenes.size() - 1) {
            indiceImagen++;
            mostrarImagenGrande(rutasImagenes.get(indiceImagen));
        }
    }//GEN-LAST:event_botonSiguienteActionPerformed

    private void botonCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonCloseActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_botonCloseActionPerformed

    private void botonAbrirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonAbrirActionPerformed
        abrirArchivo();
    }//GEN-LAST:event_botonAbrirActionPerformed

    private void botonAgregarImagenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonAgregarImagenActionPerformed
        agregarImagen();
    }//GEN-LAST:event_botonAgregarImagenActionPerformed

    private void botonGuardarAlbumActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonGuardarAlbumActionPerformed
        guardarAlbum();
    }//GEN-LAST:event_botonGuardarAlbumActionPerformed

    private void botonGuardarAlbumComoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonGuardarAlbumComoActionPerformed
        guardarAlbumComo();
    }//GEN-LAST:event_botonGuardarAlbumComoActionPerformed

    private void panelListaMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panelListaMouseClicked
        int indice = listaImagenes.getSelectedIndex();
        if (indice != -1) {
            String ruta = rutasImagenes.get(indice);
            mostrarImagenGrande(ruta);
        }
    }//GEN-LAST:event_panelListaMouseClicked

    private void botonLoopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonLoopActionPerformed
        loopInfinito = !loopInfinito;
        if (loopInfinito) {
            botonLoop.setText("Desactivar Loop Infinito");
            playAlbum();
            botonAtras.setEnabled(false);
            botonSiguiente.setEnabled(false);
            botonIrPrimera.setEnabled(false);
            botonIrUltima.setEnabled(false);
            botonPlay.setEnabled(false);
            botonPause.setEnabled(false);
            botonDetener.setEnabled(false);
        } else {
            botonLoop.setText("Loop Infinito");
            pauseAlbum();
            botonAtras.setEnabled(true);
            botonSiguiente.setEnabled(true);
            botonIrPrimera.setEnabled(true);
            botonIrUltima.setEnabled(true);
            botonPlay.setEnabled(true);
            botonPause.setEnabled(true);
            botonDetener.setEnabled(true);
        }
    }//GEN-LAST:event_botonLoopActionPerformed

    private void botonPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonPauseActionPerformed
        if (botonPause.getText().equals("Pausar")) {
            pauseAlbum();
            botonPause.setText("Reanudar");
        } else {
            reanudarAlbum();
            botonPause.setText("Pausar");
        }
    }//GEN-LAST:event_botonPauseActionPerformed


    private void botonUsoManualActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonUsoManualActionPerformed
        usoManual = !usoManual;
        if (usoManual) {
            botonUsoManual.setText("Desactivar Uso Manual");
            pauseAlbum();
            botonAtras.setEnabled(true);
            botonSiguiente.setEnabled(true);
            botonIrPrimera.setEnabled(true);
            botonIrUltima.setEnabled(true);
        } else {
            botonUsoManual.setText("Uso Manual");
            playAlbum();
            botonAtras.setEnabled(false);
            botonSiguiente.setEnabled(false);
            botonIrPrimera.setEnabled(false);
            botonIrUltima.setEnabled(false);
        }
    }//GEN-LAST:event_botonUsoManualActionPerformed

    private void botonIrUltimaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_botonIrUltimaActionPerformed
        indiceImagen = rutasImagenes.size() - 1;
        mostrarImagenGrande(rutasImagenes.get(indiceImagen));
    }//GEN-LAST:event_botonIrUltimaActionPerformed

    private void jListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jListMouseClicked
        int indice = listaImagenes.getSelectedIndex();
        if (indice != -1) {
            String ruta = rutasImagenes.get(indice);
            mostrarImagenGrande(ruta);
        }
    }//GEN-LAST:event_jListMouseClicked


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton botonAbrir;
    private javax.swing.JButton botonAgregarImagen;
    private javax.swing.JButton botonAtras;
    private javax.swing.JButton botonClose;
    private javax.swing.JButton botonDetener;
    private javax.swing.JButton botonGuardarAlbum;
    private javax.swing.JButton botonGuardarAlbumComo;
    private javax.swing.JButton botonIrPrimera;
    private javax.swing.JButton botonIrUltima;
    private javax.swing.JButton botonLoop;
    private javax.swing.JButton botonPause;
    private javax.swing.JButton botonPlay;
    private javax.swing.JButton botonSiguiente;
    private javax.swing.JButton botonUsoManual;
    public javax.swing.JList<String> jList;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel labelImagenGrande;
    private javax.swing.JPanel panelImagenGrande;
    private javax.swing.JPanel panelLista;
    // End of variables declaration//GEN-END:variables
}
