package com.drawmetry.docii3gpp;

import com.drawmetry.docii3gpp.database.DataAccessObject;
import com.inet.jortho.SpellChecker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * This is the main class of the application. It instantiates a JFrame and all
 * its children components.
 * 
 * @author Erik Colban &copy; 2012 <br>
 *         All Rights Reserved Worldwide
 */
@SuppressWarnings("serial")
public class UI extends JFrame implements Runnable, ClipboardOwner {

	private static final String REVISION = "Revision 1.0.1 (2013-04-25)";
	private static final String DATABASE_LOCATION = "Database Location";
	private static final String ENTRY_NOT_FOUND_IN_DATABASE = "Entry not found in database";
	private static final String NO_ENTRY_SELECTED = "No entry selected";
	private static final String ENTRIES_EXPORTED_TO = "Entries exported to";
	private static final String EXCEL = "Excel";
	private static final String DOES_NOT_EXIST = " does not exist";
	private static final String DATABASE_URL = "Database URL";
	private static final String FILTER = "Search";
	private static final String FILTER_TOOL_TIP_TEXT = "Narrow down the selection";
	private static final String SYNCHRONIZE = "Sync metadata";
	private static final String SYNCHRONIZE_TOOL_TIP_TEXT = "Get document information for current meeting";
	private static final String DOWNLOAD = "Download";
	private static final String DOWNLOAD_TOOL_TIP_TEXT = "Download files for current meeting";
	private static final String EXPORT = "Export";
	private static final String EXPORT_TOOL_TIP_TEXT = "Export entries to an Excel file";
	private static final String ABOUT = "About";
	public static final Logger LOGGER = Logger
			.getLogger("com.drawmetry.docii3gpp");

	private static String[] meetings;
	private String currentMeeting;

	private DocEntry currentEntry = null;
	private boolean syncLock = false;
	private FilterDialog filterDialog;
	private List<DocEntry> allEntries;
	// private List<DocEntry> latestEntries;
	private Synchronizer synchronizer;
	private Downloader downLoader;

	private JTextField titleTextField;
	private JTextField sourceTextField;
	private JTextField agendaTitleTextField;
	private JTextField workItemTextField;
	private JTextField commentTextField;
	private JTextField revByTextField;
	private JTextField revOfTextField;
	private JTextField decisionTextField;
	private JTextArea notesArea;
	private JTextArea outputArea;
	private JTabbedPane docTabbedPane;
	private JList<DocEntry> documentList;

	private JComboBox<String> meetingComboBox;
	private JButton synchronizeButton;
	private JButton downloadButton;
	private JButton stopButton;

	private JProgressBar downloadProgressBar;

	private class PopupListener extends MouseAdapter {

		JPopupMenu popup;

		PopupListener(JPopupMenu popupMenu) {
			popup = popupMenu;
		}

		@Override
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		private void maybeShowPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}

	private DataAccessObject db;
	/* State */
	private boolean changed = false; // true when entry needs to be saved
	// private boolean latest = false; // true when only the latest revision is
	// to
	// appear in the entry list
	private DocumentListener documentChangeListener = new DocumentListener() {
		@Override
		public void insertUpdate(DocumentEvent e) {
			changed = true;
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			changed = true;
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			changed = true;
		}
	};

	private class DocEntryRenderer extends DefaultListCellRenderer {

		/**
		 * Creates a new instance of DocEntryRenderer
		 */
		public DocEntryRenderer() {
		}

		@Override
		public Component getListCellRendererComponent(JList<?> list,
				Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected,
					cellHasFocus);

			DocEntry entry = (DocEntry) value;
			// String fn = entry.getFileName();
			File file = Configuration.getLocalFile(currentMeeting,
					entry.getFileName());
			if (file.exists()) {
				this.setForeground(Color.BLACK);
			} else {
				this.setForeground(Color.GRAY);
			}
			this.setText(entry.getFileName());
			return this;
		}
	}

	public UI() {
		meetings = Configuration.getMeetings();
		this.currentMeeting = meetings[meetings.length - 1];
	}

	private void initComponents() {

		setTitle("Docii 3GPP Edition");

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(1020, 530));
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				formWindowClosing(evt);
			}
		});

		/*
		 * Menu Bar
		 */
		JMenuBar menuBar = buildMenuBar();

		setJMenuBar(menuBar);
		/* ******************* End of menu bar ************************** */

		setLayout(new BorderLayout());
		/*
		 * Tool Bar
		 */

		JToolBar toolBar = buildToolbar();
		add(toolBar, BorderLayout.NORTH);

		/* End of too bar */

		/* Scroll pane for the list of documents */

		JSplitPane mainSplitPane = new JSplitPane();
		mainSplitPane.setPreferredSize(new Dimension(900, 405));
		JScrollPane documentListScrollPane = buildDocumentListScrollPane();
		mainSplitPane.setLeftComponent(documentListScrollPane);

		JSplitPane subSplitPane = new JSplitPane();
		subSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

		docTabbedPane = new JTabbedPane();
		JPanel docPanel = new JPanel();
		docPanel = new JPanel();
		docPanel.setFont(new Font("Arial", 0, 18)); // NOI18N
		docPanel.setMinimumSize(new Dimension(400, 300));
		docPanel.setPreferredSize(new Dimension(600, 300));
		docPanel.setLayout(new GridBagLayout());

		JPanel docFieldsPanel = new JPanel();
		docFieldsPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		Insets insets = new Insets(5, 5, 5, 5);
		int row = 0;

		// title
		JLabel titleLabel = new JLabel();
		titleLabel.setFont(new Font("SansSerif", 0, 11)); // NOI18N
		titleLabel.setText("Title:"); // NOI18N
		c.anchor = GridBagConstraints.LINE_START;
		c.gridx = 0;
		c.gridy = row;
		c.weightx = 0.0;
		c.insets = insets;
		docFieldsPanel.add(titleLabel, c);

		titleTextField = new JTextField(40);
		c.gridx = 1;
		c.gridy = row;
		c.weightx = 1.0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		docFieldsPanel.add(titleTextField, c);
		row++;

		// source

		JLabel sourceLabel = new JLabel();
		sourceLabel.setFont(new Font("SansSerif", 0, 11)); // NOI18N
		sourceLabel.setText("Source:"); // NOI18N
		c.gridx = 0;
		c.gridy = row;
		c.weightx = 0.0;
		c.insets = insets;
		docFieldsPanel.add(sourceLabel, c);

		sourceTextField = new JTextField(40);
		c.gridx = 1;
		c.gridy = row;
		c.weightx = 1.0;
		c.gridwidth = 2;
		docFieldsPanel.add(sourceTextField, c);
		row++;

		// agenda title
		JLabel agendaLabel = new JLabel();
		agendaLabel.setFont(new Font("SansSerif", 0, 11)); // NOI18N
		agendaLabel.setText("Agenda:"); // NOI18N
		c.gridx = 0;
		c.gridy = row;
		c.weightx = 0.0;
		c.insets = insets;
		docFieldsPanel.add(agendaLabel, c);

		agendaTitleTextField = new JTextField(40);
		c.gridx = 1;
		c.gridy = row;
		c.weightx = 1.0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		docFieldsPanel.add(agendaTitleTextField, c);
		row++;

		// work item
		JLabel workItemLabel = new JLabel();
		workItemLabel.setFont(new Font("SansSerif", 0, 11)); // NOI18N
		workItemLabel.setText("Work Item:"); // NOI18N
		c.gridx = 0;
		c.gridy = row;
		c.weightx = 0.0;
		c.insets = insets;
		docFieldsPanel.add(workItemLabel, c);

		workItemTextField = new JTextField(40);
		c.gridx = 1;
		c.gridy = row;
		c.weightx = 1.0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		docFieldsPanel.add(workItemTextField, c);
		row++;

		// comment
		JLabel commentLabel = new JLabel();
		commentLabel.setFont(new Font("SansSerif", 0, 11)); // NOI18N
		commentLabel.setText("Comment:"); // NOI18N
		c.gridx = 0;
		c.gridy = row;
		c.weightx = 0.0;
		c.insets = insets;
		docFieldsPanel.add(commentLabel, c);

		commentTextField = new JTextField(40);
		c.gridx = 1;
		c.gridy = row;
		c.weightx = 1.0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		docFieldsPanel.add(commentTextField, c);
		row++;

		// revised by / revision of
		JLabel revLabel = new JLabel();
		revLabel.setFont(new Font("SansSerif", 0, 11)); // NOI18N
		revLabel.setText("Rev from / to:"); // NOI18N
		c.gridx = 0;
		c.gridy = row;
		c.gridwidth = 1;
		c.weightx = 0.0;
		c.insets = insets;
		docFieldsPanel.add(revLabel, c);

		revOfTextField = new JTextField(40);
		c.gridx = 1;
		c.gridy = row;
		c.weightx = 1.0;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		docFieldsPanel.add(revOfTextField, c);

		revByTextField = new JTextField(40);
		c.gridx = 2;
		c.gridy = row;
		c.weightx = 1.0;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		docFieldsPanel.add(revByTextField, c);
		row++;

		// decision
		JLabel decisionLabel = new JLabel();
		decisionLabel.setFont(new Font("SansSerif", 0, 11)); // NOI18N
		decisionLabel.setText("Decision:"); // NOI18N
		c.gridx = 0;
		c.gridy = row;
		c.weightx = 0.0;
		c.insets = insets;
		docFieldsPanel.add(decisionLabel, c);

		decisionTextField = new JTextField(40);
		c.gridx = 1;
		c.gridy = row;
		c.weightx = 1.0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		docFieldsPanel.add(decisionTextField, c);
		row++;

		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.0;
		docPanel.add(docFieldsPanel, c);

		/* Notes */
		JScrollPane notesScrollPane = new JScrollPane();
		notesScrollPane
				.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		notesArea = new JTextArea();
		notesArea.setLineWrap(true);
		notesArea.setRows(7);
		notesArea.setToolTipText("Notes"); // NOI18N
		notesArea.setWrapStyleWord(true);
		URL url = UI.class.getResource("dict/");
		SpellChecker.registerDictionaries(url, "en", null, ".ortho");
		SpellChecker.register(notesArea);

		notesScrollPane.setViewportView(notesArea);

		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		docPanel.add(notesScrollPane, c);

		docTabbedPane.add(docPanel);
		subSplitPane.setTopComponent(docTabbedPane);

		/* Output */
		JScrollPane outputScrollPane = new JScrollPane();
		outputArea = new JTextArea();

		outputScrollPane
				.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		outputScrollPane.setMaximumSize(new Dimension(32767, 50));
		outputScrollPane.setMinimumSize(new Dimension(23, 50));

		outputArea.setColumns(20);
		outputArea.setEditable(false);
		outputArea.setRows(5);
		outputArea.setToolTipText("Messages"); // NOI18N
		outputScrollPane.setViewportView(outputArea);

		subSplitPane.setBottomComponent(outputScrollPane);

		mainSplitPane.setRightComponent(subSplitPane);

		add(mainSplitPane, BorderLayout.CENTER);

		downloadProgressBar = new JProgressBar();
		add(downloadProgressBar, BorderLayout.SOUTH);

		pack();

		filterDialog = new FilterDialog(this, true);
	}

	private JScrollPane buildDocumentListScrollPane() {
		JScrollPane documentScrollPane = new JScrollPane();
		documentList = new JList<DocEntry>();

		documentScrollPane.setPreferredSize(new Dimension(200, 200));

		documentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		documentList.setToolTipText("Doble-click to open document"); // NOI18N
		documentList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2) {
					openCurrentDocument();
				}
			}
		});
		documentList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent evt) {
				documentListValueChanged(evt);
			}
		});
		documentScrollPane.setViewportView(documentList);
		return documentScrollPane;
	}

	private JPopupMenu buildPopupMenu() {
		/*
		 * Pop Up Menu
		 */
		JPopupMenu popupMenu = new JPopupMenu();

		JMenuItem copyDocId = new JMenuItem();
		copyDocId.setText("Copy TDoc #"); // NOI18N
		copyDocId.setToolTipText("");
		copyDocId.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				copyDocIdActionPerformed(evt);
			}
		});
		popupMenu.add(copyDocId);

		JMenuItem copyURL = new JMenuItem();
		copyURL.setText("Copy URL"); // NOI18N
		copyURL.setToolTipText("Copy URL"); // NOI18N
		copyURL.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				copyURLActionPerformed(evt);
			}
		});
		popupMenu.add(copyURL);

		JMenuItem deleteEntry = new JMenuItem();
		deleteEntry.setText("Delete entry"); // NOI18N
		deleteEntry.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				deleteEntryActionPerformed(evt);
			}
		});
		popupMenu.add(deleteEntry);
		return popupMenu;
	}

	private JMenuBar buildMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		/* File Menu */
		JMenu fileMenu = new JMenu();
		JMenuItem openMenuItem = new JMenuItem();
		openMenuItem.setText("Open document"); // NOI18N
		openMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				openMenuItemActionPerformed(evt);
			}
		});
		fileMenu.add(openMenuItem);

		JMenuItem exitMenuItem = new JMenuItem();
		fileMenu.setText("File"); // NOI18N
		fileMenu.setActionCommand("File Menu"); // NOI18N

		exitMenuItem.setText("Exit"); // NOI18N
		exitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				exitMenuItemActionPerformed(evt);
			}
		});
		fileMenu.add(exitMenuItem);

		menuBar.add(fileMenu);

		/* Edit Menu */
		JMenu editMenu = new JMenu();
		editMenu.setText("Edit"); // NOI18N
		JMenuItem copyFileNameMenuItem = new JMenuItem();
		copyFileNameMenuItem.setText("Copy TDoc #"); // NOI18N
		copyFileNameMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				copyDocIdActionPerformed(evt);
			}
		});
		editMenu.add(copyFileNameMenuItem);

		JMenuItem copyUrlMenuItem = new JMenuItem();
		copyUrlMenuItem.setText("Copy URL"); // NOI18N
		copyUrlMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				copyURLActionPerformed(evt);
			}
		});
		editMenu.add(copyUrlMenuItem);

		JMenuItem deleteEntryMenuItem = new JMenuItem();
		deleteEntryMenuItem.setText("Delete entry"); // NOI18N
		deleteEntryMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				deleteEntryActionPerformed(evt);
			}
		});
		editMenu.add(deleteEntryMenuItem);
		JMenuItem deleteFileMenuItem = new JMenuItem();
		deleteFileMenuItem.setText("Delete file"); // NOI18N
		deleteFileMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				deleteFileMenuItemActionPerformed(evt);
			}
		});
		editMenu.add(deleteFileMenuItem);

		menuBar.add(editMenu);

		/* Help Menu */

		JMenu helpMenu = new JMenu();
		helpMenu.setText("Help"); // NOI18N

		JMenuItem helpMenuItem = new JMenuItem();
		helpMenuItem.setText("Help"); // NOI18N
		helpMenuItem.setEnabled(false);
		helpMenu.add(helpMenuItem);
		JMenuItem aboutMenuItem = new JMenuItem();
		aboutMenuItem.setText("About"); // NOI18N
		aboutMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				aboutMenuItemActionPerformed(evt);
			}
		});
		helpMenu.add(aboutMenuItem);

		menuBar.add(helpMenu);
		return menuBar;
	}

	private JToolBar buildToolbar() {
		JToolBar toolBar = new JToolBar();

		meetingComboBox = new JComboBox<String>();
		meetingComboBox.setFont(new Font("SansSerif", 0, 12)); // NOI18N
		meetingComboBox.setModel(new DefaultComboBoxModel<String>(meetings));
		meetingComboBox.setMaximumSize(new Dimension(200, 20));
		meetingComboBox.setMinimumSize(new Dimension(100, 20));
		meetingComboBox.setPreferredSize(new Dimension(150, 20));
		meetingComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				meetingComboBoxActionPerformed(evt);
			}
		});
		toolBar.add(meetingComboBox);

		JButton filterButton = new JButton();
		filterButton.setFont(new Font("SansSerif", 0, 12)); // NOI18N
		filterButton.setText(FILTER + "...");
		filterButton.setToolTipText(FILTER_TOOL_TIP_TEXT);
		filterButton.setBorder(BorderFactory
				.createBevelBorder(BevelBorder.RAISED));
		filterButton.setFocusable(false);
		filterButton.setHorizontalTextPosition(SwingConstants.CENTER);
		filterButton.setMaximumSize(new Dimension(200, 21));
		filterButton.setMinimumSize(new Dimension(80, 21));
		filterButton.setPreferredSize(new Dimension(80, 21));
		filterButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		filterButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				filterButtonActionPerformed(evt);
			}
		});
		toolBar.add(filterButton);

		synchronizeButton = new JButton();
		synchronizeButton.setFont(new Font("SansSerif", 0, 12)); // NOI18N
		synchronizeButton.setText(SYNCHRONIZE);
		synchronizeButton.setToolTipText(SYNCHRONIZE_TOOL_TIP_TEXT);
		synchronizeButton.setBorder(BorderFactory
				.createBevelBorder(BevelBorder.RAISED));
		synchronizeButton.setFocusable(false);
		synchronizeButton.setHorizontalTextPosition(SwingConstants.CENTER);
		synchronizeButton.setMaximumSize(new Dimension(200, 21));
		synchronizeButton.setMinimumSize(new Dimension(80, 21));
		synchronizeButton.setPreferredSize(new Dimension(80, 21));
		synchronizeButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		synchronizeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				synchronizeButtonActionPerformed(evt);
			}
		});
		toolBar.add(synchronizeButton);

		downloadButton = new JButton();
		downloadButton.setFont(new Font("SansSerif", 0, 12)); // NOI18N
		downloadButton.setText(DOWNLOAD);
		downloadButton.setToolTipText(DOWNLOAD_TOOL_TIP_TEXT);
		downloadButton.setBorder(BorderFactory
				.createBevelBorder(BevelBorder.RAISED));
		downloadButton.setFocusable(false);
		downloadButton.setHorizontalTextPosition(SwingConstants.CENTER);
		downloadButton.setMaximumSize(new Dimension(200, 21));
		downloadButton.setMinimumSize(new Dimension(80, 21));
		downloadButton.setPreferredSize(new Dimension(80, 21));
		downloadButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		downloadButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				downloadButtonActionPerformed(evt);
			}
		});
		toolBar.add(downloadButton);

		JButton exportButton = new JButton();
		exportButton.setFont(new Font("SansSerif", 0, 12)); // NOI18N
		exportButton.setText(EXPORT);
		exportButton.setToolTipText(EXPORT_TOOL_TIP_TEXT);
		exportButton.setBorder(BorderFactory
				.createBevelBorder(BevelBorder.RAISED));
		exportButton.setFocusable(false);
		exportButton.setHorizontalTextPosition(SwingConstants.CENTER);
		exportButton.setMaximumSize(new Dimension(200, 21));
		exportButton.setMinimumSize(new Dimension(80, 21));
		exportButton.setPreferredSize(new Dimension(80, 21));
		exportButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		exportButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				exportButtonActionPerformed(evt);
			}
		});
		toolBar.add(exportButton);

		stopButton = new JButton();
		stopButton.setIcon(new ImageIcon(getClass().getResource(
				"/com/drawmetry/docii3gpp/images/stop2_32.png"))); // NOI18N
		stopButton.setBorder(null);
		stopButton.setIconTextGap(0);
		stopButton.setMaximumSize(new Dimension(32, 22));
		stopButton.setMinimumSize(new Dimension(32, 22));
		stopButton.setPreferredSize(new Dimension(32, 22));
		stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				stopButtonActionPerformed(evt);
			}
		});
		toolBar.add(stopButton);
		return toolBar;
	}

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String args[]) {
		Configuration.initialize();
		SwingUtilities.invokeLater(new UI());
	}

	@Override
	public void run() {
		initComponents();
		LOGGER.addHandler(new LogHandler(outputArea));
		db = new DataAccessObject();
		LOGGER.log(Level.INFO, "{0}: {1}\n", new Object[] { DATABASE_LOCATION,
				getDb().getDatabaseLocation() });
		LOGGER.log(Level.INFO, "{0}: {1}\n", new Object[] { DATABASE_URL,
				getDb().getDatabaseUrl() });
		if (db.connect()) {
			initComponentsContinued();
			meetingComboBox.setSelectedIndex(0);
			setVisible(true);
		} else {
			System.exit(1);
		}
	}

	private void initComponentsContinued() {
		Image icon = null;
		try {
			URL iconURL = UI.class.getResource("images/consensii.png");
			if (iconURL != null) {
				icon = ImageIO.read(iconURL);
			} else {
				System.err.println("Cannot load logo.");
			}
		} catch (IOException ex) {
			System.err.println("Cannot load logo.");
		}
		if (icon != null) {
			setIconImage(icon);
		}
		stopButton.setEnabled(false);

		documentList.setCellRenderer(new DocEntryRenderer());
		documentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		documentList.addMouseListener(new PopupListener(buildPopupMenu()));

		notesArea.getDocument().addDocumentListener(documentChangeListener);
	}

	private void selectMeeting(int i) {
		assert 0 <= i && i < meetings.length;

		currentMeeting = meetings[i];
		filter();
		updateDocumentList();
	}

	private void documentListValueChanged(ListSelectionEvent evt) {
		if (evt.getValueIsAdjusting()) {
			return;
		}
		DocEntry entry = documentList.getSelectedValue();
		if (changed && currentEntry != null) {
			save(currentEntry);
		}
		changed = false;
		currentEntry = entry;
		if (entry != null) {
			int id = entry.getId();
			DocumentObject docObj = getDb().getDocumentOject(getTable(), id);
			if (docObj != null) {
				fillDocumentFields(docObj);
			} else {
				clearDocumentFields();
			}
		} else {
			clearDocumentFields();
		}
	}

	private void formWindowClosing(WindowEvent evt) {
		closeDown();
	}

	private void closeDown() {
		if (changed && currentEntry != null) {
			save(currentEntry);
		}
		getDb().disconnect();
	}

	private void openCurrentDocument() {
		if (currentEntry == null) {
			return;
		}
		DocumentObject docObj = getDb().getDocumentOject(getTable(),
				currentEntry.getId());
		assert docObj != null;
		String fileName = docObj.getTDoc();
		assert fileName != null;
		File file = Configuration.getLocalFile(currentMeeting, fileName);
		if (!file.exists()) {
			int answer = JOptionPane.showConfirmDialog(this,
					"File not found locally. Download now?", "Download",
					JOptionPane.YES_NO_OPTION);
			if (answer == JOptionPane.YES_OPTION) {
				Downloader dl = new Downloader(this);
				dl.downloadNow(docObj);
				documentList.repaint();
			}
		}
		if (file.exists()) {
			try {
				file = file.getCanonicalFile();
				Desktop.getDesktop().open(file);
			} catch (NullPointerException ex) {
				LOGGER.log(Level.SEVERE, "{0}\n", ex.getMessage());
			} catch (UnsupportedOperationException ex) {
				LOGGER.log(Level.SEVERE, "{0}\n", ex.getMessage());
			} catch (SecurityException ex) {
				LOGGER.log(Level.SEVERE, "{0}\n", ex.getMessage());
			} catch (IOException ex) {
				LOGGER.log(Level.SEVERE, "{0}\n", ex.getMessage());
			} catch (IllegalArgumentException ex) {
				LOGGER.log(Level.SEVERE, "{0} {1}.\n", new String[] { fileName,
						DOES_NOT_EXIST });
			}
		}
	}

	private void filter() {
		allEntries = db.findEntries(getTable(), currentMeeting, "%", "%", "%",
				"%", "%", "%", "%");
	}

	private void filterButtonActionPerformed(ActionEvent evt) {
		filterDialog.setVisible(true);
		allEntries = filterDialog.getEntries();
		updateDocumentList();
	}

	private void synchronizeButtonActionPerformed(ActionEvent evt) {
		if (!isSyncLock()) {
			setSyncLock(true);
			URL url = Configuration.getTDocList(currentMeeting);
			synchronizer = new Synchronizer(this, url);
			Thread syncThread = new Thread(synchronizer);
			syncThread.start();
		}
		filter();
		updateDocumentList();
	}

	private void downloadButtonActionPerformed(ActionEvent evt) {
		if (!isSyncLock()) {
			setSyncLock(true);
			downLoader = new Downloader(this);
			Thread downloadThread = new Thread(downLoader);
			downloadThread.start();
			stopButton.setEnabled(true);
		}
	}

	private void exportButtonActionPerformed(ActionEvent evt) {

		FileOutputStream fileOut = null;
		try {
			JFileChooser chooser = new JFileChooser();
			FileNameExtensionFilter filter = new FileNameExtensionFilter(EXCEL,
					"xls");
			chooser.setFileFilter(filter);
			int returnVal = chooser.showSaveDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = chooser.getSelectedFile();
				String fileName = file.getName();
				String path = file.getParent();
				int pos = fileName.lastIndexOf(".");
				if (!"xls".equals(fileName.substring(pos + 1))) {
					fileName = fileName + ".xls";
					file = new File(path, fileName);
				}
				fileOut = new FileOutputStream(file);
				new Exporter(this).write(fileOut);
				LOGGER.log(Level.INFO, "{0} {1}.\n", new Object[] {
						ENTRIES_EXPORTED_TO, file.getAbsolutePath() });
			}
		} catch (IOException ex) {
			LOGGER.log(Level.WARNING, "{0}\n", ex.getMessage());
		} finally {
			if (fileOut != null) {
				try {
					fileOut.close();
				} catch (IOException ex) {
				}
			}
		}
	}

	private void stopButtonActionPerformed(ActionEvent evt) {
		if (isSyncLock()) {
			stopButton.setEnabled(false);
			if (downLoader != null) {
				downLoader.setAbort(true);
				downloadButton.setEnabled(false);
			}
			if (synchronizer != null) {
				synchronizer.setAbort(true);
				synchronizeButton.setEnabled(false);
			}
		}
	}

	private void deleteEntryActionPerformed(ActionEvent evt) {
		int selectedIndex = documentList.getSelectedIndex();
		if (selectedIndex >= 0) {
			ListModel<DocEntry> model = documentList.getModel();
			currentEntry = model.getElementAt(selectedIndex);
			if (currentEntry != null) {
				db.deleteRecord(getTable(), currentEntry.getId());
				allEntries.remove(currentEntry);
				// if (latestEntries != null) {
				// latestEntries.remove(currentEntry);
				// }
				updateDocumentList();
			}
		}
	}

	private void copyURLActionPerformed(ActionEvent evt) {
		Clipboard clipboard = getToolkit().getSystemClipboard();

		if (currentEntry != null) {
			int id = currentEntry.getId();
			DocumentObject obj = db.getDocumentOject(getTable(), id);
			if (obj != null) {
				StringSelection selection = new StringSelection(obj.getUrl()
						.toString());
				clipboard.setContents(selection, this);
			}
		}
	}

	private void copyDocIdActionPerformed(ActionEvent evt) {
		Clipboard clipboard = getToolkit().getSystemClipboard();

		if (currentEntry != null) {
			int id = currentEntry.getId();
			DocumentObject obj = db.getDocumentOject(getTable(), id);
			if (obj != null) {
				StringSelection selection = new StringSelection(obj.getTDoc());
				clipboard.setContents(selection, this);
			}
		}
	}

	private void aboutMenuItemActionPerformed(ActionEvent evt) {
		System.setProperty("awt.useSystemAAFontSettings", "on");
		final JEditorPane editorPane = new JEditorPane();

		// Enable use of custom set fonts
		editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,
				Boolean.TRUE);
		editorPane.setFont(new Font("Arial", Font.BOLD, 13));

		editorPane.setPreferredSize(new Dimension(520, 180));
		editorPane.setEditable(false);
		editorPane.setContentType("text/html");
		editorPane
				.setText("<html>"
						+ "<body>"
						+ "<table border='0px' cxellpadding='10px' height='100%'>"
						+ "<tr>"
						+ "<td valign='center'>"
						+ "<img src=\""
						+ UI.class.getResource("images/consensii.png")
								.toExternalForm()
						+ "\">"
						+ "</td>"
						+ "<td align=center>"
						+ "Docii&trade; Document Manager &ndash; "
						+ "3GPP Edition<br>"
						+ REVISION
						+ "<br/>"
						+ "<br/>"
						+ "Copyright &copy; 2013 <a href=\"mailto:support@drawmetry.com\">Erik Colban</a><br>"
						+ "All Rights Reserved Worldwide<p>"
						+ "<br/>"
						+ "Docii is a trademark of Consensii LLC.<br>"
						+ "<a href=\"http://consensii.com\"><b>consensii.com</b></a><br>"
						+ "</td>" + "</tr>" + "</table>" + "</body>"
						+ "</html>");

		// TIP: Add Hyperlink listener to process hyperlinks
		editorPane.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(final HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
					EventQueue.invokeLater(new Runnable() {
						@Override
						public void run() {
							// TIP: Show hand cursor
							SwingUtilities
									.getWindowAncestor(editorPane)
									.setCursor(
											Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
							// TIP: Show URL as the tooltip
							editorPane.setToolTipText(e.getURL()
									.toExternalForm());
						}
					});
				} else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							// Show default cursor
							SwingUtilities.getWindowAncestor(editorPane)
									.setCursor(Cursor.getDefaultCursor());

							// Reset tooltip
							editorPane.setToolTipText(null);
						}
					});
				} else if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					// TIP: Starting with JDK6 you can show the URL in desktop
					// browser
					if (Desktop.isDesktopSupported()) {
						try {
							Desktop.getDesktop().browse(e.getURL().toURI());
						} catch (Exception ex) {
							LOGGER.log(Level.SEVERE, "Cannot find the browser");
						}
					}
				}
			}
		});
		JOptionPane.showMessageDialog(null, new JScrollPane(editorPane), ABOUT,
				JOptionPane.PLAIN_MESSAGE);
	}

	private void openMenuItemActionPerformed(ActionEvent evt) {
		openCurrentDocument();
	}

	private void exitMenuItemActionPerformed(ActionEvent evt) {
		closeDown();
		System.exit(0);
	}

	private void deleteCurrentFile() {
		if (currentEntry == null) {
			return;
		}
		DocumentObject docObj = getDb().getDocumentOject(getTable(),
				currentEntry.getId());
		assert docObj != null;
		String fileName = docObj.getTDoc();
		assert fileName != null;
		File file = Configuration.getLocalFile(currentMeeting, fileName);
		if (file.exists()) {
			file.delete();
			documentList.repaint();
		}
	}

	private void deleteFileMenuItemActionPerformed(ActionEvent evt) {
		deleteCurrentFile();
	}

	private void updateDocumentList() {
		DocEntry entry = (DocEntry) documentList.getSelectedValue();
		int id = -1;
		if (entry != null) {
			id = entry.getId();
		}
		if (allEntries == null) {
			return;
		}
		refreshListEntries();
		List<DocEntry> modelEntries = allEntries;
		if (id != -1) {
			for (int index = 0; index < modelEntries.size(); index++) {
				if (modelEntries.get(index).getId() == id) {
					setSelectedIndex(index);
					return;
				}
			}
		}
		setSelectedIndex(0);
	}

	private void refreshListEntries() {
		DocEntry[] data = allEntries.toArray(new DocEntry[0]);
		documentList.setListData(data);
		// return;
		// latestEntries = new ArrayList<DocEntry>(allEntries.size());
		// DocEntry prevEntry = null;
		// for (DocEntry entry : allEntries) {
		// int i = entry.compare(prevEntry);
		// if (prevEntry == null || i > 0) {
		// prevEntry = entry;
		// } else if (i == 0) {
		// latestEntries.add(prevEntry);
		// prevEntry = entry;
		// }
		// }
		// if (prevEntry != null) {
		// latestEntries.add(prevEntry);
		// }
		// DocEntry[] data = latestEntries.toArray(new DocEntry[0]);
		// documentList.setListData(data);

	}

	public int getSelectedIndex() {
		return documentList.getSelectedIndex();
	}

	private int setSelectedIndex(int index) {
		assert index >= -1;
		ListModel<DocEntry> model = documentList.getModel();
		int size = model.getSize();
		if (index < size) {
			documentList.setSelectedIndex(index);
		} else {
			documentList.setSelectedIndex(size - 1);
			index = size - 1;
		}
		if (changed && currentEntry != null) {
			save(currentEntry);
		}
		if (model.getSize() > 0) {
			currentEntry = (DocEntry) model.getElementAt(index);
		}
		changed = false;
		return index;
	}

	public DocEntry getSelectedListEntry() {
		DocEntry entry = (DocEntry) documentList.getSelectedValue();
		return entry;
	}

	// End of variables declaration

	private void fillDocumentFields(DocumentObject fo) {

		titleTextField.setText(fo.getDocTitle());
		sourceTextField.setText(fo.getSource());
		agendaTitleTextField.setText(fo.getAgendaTitle());
		commentTextField.setText(fo.getComment());
		workItemTextField.setText(fo.getWorkItem());
		revByTextField.setText(fo.getRevByTDoc());
		revOfTextField.setText(fo.getRevOfTDoc());
		decisionTextField.setText(fo.getDecision());
		notesArea.setText(fo.getNotes());
		docTabbedPane.setTitleAt(0, fo.getTDoc());
		changed = false;
	}

	private void clearDocumentFields() {
		titleTextField.setText("");
		sourceTextField.setText("");
		agendaTitleTextField.setText("");
		commentTextField.setText("");
		workItemTextField.setText("");
		revByTextField.setText("");
		revOfTextField.setText("");
		decisionTextField.setText("");
		notesArea.setText("");
		docTabbedPane.setTitleAt(0, "");
	}

	private void save(DocEntry entry) {
		if (entry != null) {
			int id = entry.getId();
			DocumentObject oldFo = getDb().getDocumentOject(getTable(), id);
			if (oldFo == null) {
				LOGGER.log(Level.WARNING, "{0}.\n", ENTRY_NOT_FOUND_IN_DATABASE);
				return;
			}
			try {
				URL oldUrl = oldFo.getUrl();
				DocumentObject newFo = new DocumentObject(id,
						oldFo.getMeeting(), oldFo.getAgendaItem(),
						oldFo.getAgendaTitle(), oldUrl == null ? null
								: oldUrl.toString(), oldFo.getTDoc(),
						oldFo.getDocType(), oldFo.getDocTitle(),
						oldFo.getSource(), oldFo.getWorkItem(),
						oldFo.getRevByTDoc(), oldFo.getRevOfTDoc(),
						oldFo.getLsSource(), oldFo.getComment(),
						oldFo.getDecision(), notesArea.getText());
				getDb().editRecord(getTable(), id, newFo);
			} catch (MalformedURLException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		} else {
			LOGGER.log(Level.WARNING, "{0}.\n", NO_ENTRY_SELECTED);
		}
	}

	/**
	 * @return the db
	 */
	public DataAccessObject getDb() {
		return db;
	}

	/**
	 * @return the syncLock
	 */
	public boolean isSyncLock() {
		return syncLock;
	}

	/**
	 * @param lock
	 *            the syncLock to set
	 */
	public void setSyncLock(boolean lock) {

		synchronizeButton.setEnabled(!lock);
		downloadButton.setEnabled(!lock);
		stopButton.setEnabled(lock);
		this.syncLock = lock;
		if (!lock) {
			filter();
			updateDocumentList();
		}
	}

	public List<DocumentObject> getDocsToDownload() {
		List<DocEntry> entries = allEntries;
		List<DocumentObject> docs = new ArrayList<DocumentObject>(
				entries.size());
		for (DocEntry e : entries) {
			docs.add(db.getDocumentOject(getTable(), e.getId()));
		}
		return docs;
	}

	public String getTable() {
		return Configuration.getTables()[0];
	}

	public DocEntry[] getEntries() {
		return allEntries.toArray(new DocEntry[0]);
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
	}

	public String getMeeting() {
		return currentMeeting;
	}

	private void meetingComboBoxActionPerformed(ActionEvent evt) {// GEN-FIRST:event_workingGroupComboBoxActionPerformed
		if (changed && currentEntry != null) {
			save(currentEntry);
		}
		documentList.clearSelection();
		int i = meetingComboBox.getSelectedIndex();
		selectMeeting(i);
	}

	public void setDownloadProgress(final int progress) {

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				downloadProgressBar.setValue(progress);
			}
		});
	}
}
