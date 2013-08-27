/*
 * FilterDialog.java
 *
 */
package com.drawmetry.docii3gpp;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import com.drawmetry.docii3gpp.database.DataAccessObject;

/**
 * A dialog for searches in the database
 * 
 * @author Erik Colban &copy; 2013 <br>
 *         All Rights Reserved Worldwide
 */
@SuppressWarnings("serial")
public class FilterDialog extends JDialog {

	private final UI parent;
	private List<DocEntry> entries;
	private ActionListener filterListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			okButtonActionPerformed(e);
		}
	};
	private JTextField tDocTextField;
	private JTextField titleTextField;
	private JTextField authorsTextField;
	private JTextField agendaTitleTextField;
	private JTextField workItemTextField;
	private JTextField decisionTextField;
	private JTextField commentTextField;
	private JTextField notesTextField;

	/**
	 * Creates new FilterDialog
	 * 
	 */
	public FilterDialog(UI parent, boolean modal) {
		super(parent, "Filter", modal);
		this.parent = parent;
		initComponents();
		tDocTextField.addActionListener(filterListener);
		titleTextField.addActionListener(filterListener);
		authorsTextField.addActionListener(filterListener);
		agendaTitleTextField.addActionListener(filterListener);
		workItemTextField.addActionListener(filterListener);
		commentTextField.addActionListener(filterListener);
		notesTextField.addActionListener(filterListener);
		decisionTextField.addActionListener(filterListener);

	}

	/**
	 * @return the entries
	 */
	public List<DocEntry> getEntries() {
		return entries;
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	private void initComponents() {

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		Insets insets = new Insets(5, 5, 5, 5);
		int row = 0;

		JLabel tDocLabel = new JLabel();
		tDocLabel.setFont(new java.awt.Font("SansSerif", 0, 11)); // NOI18N
		tDocLabel.setText("File name"); // NOI18N
		c.anchor = GridBagConstraints.LINE_START;
		c.gridx = 0;
		c.gridy = row;
		c.insets = insets;
		add(tDocLabel, c);

		tDocTextField = new JTextField(20);
		c.gridx = 1;
		c.gridy = row;
		c.weightx = 1.0;
		c.gridwidth = 1;
		add(tDocTextField, c);
		row++;

		JLabel titleLabel = new JLabel();
		titleLabel.setFont(new java.awt.Font("SansSerif", 0, 11)); // NOI18N
		titleLabel.setText("Title:"); // NOI18N
		c.anchor = GridBagConstraints.LINE_START;
		c.gridx = 0;
		c.gridy = row;
		c.insets = insets;
		add(titleLabel, c);

		titleTextField = new JTextField(40);
		c.gridx = 1;
		c.gridy = row;
		c.weightx = 1.0;
		c.gridwidth = 2;
		add(titleTextField, c);
		row++;

		JLabel authorsLabel = new JLabel();
		authorsLabel.setFont(new java.awt.Font("SansSerif", 0, 11)); // NOI18N
		authorsLabel.setText("Source:"); // NOI18N
		c.gridx = 0;
		c.gridy = row;
		c.insets = insets;
		add(authorsLabel, c);

		authorsTextField = new JTextField(40);
		c.gridx = 1;
		c.gridy = row;
		c.weightx = 1.0;
		c.gridwidth = 2;
		add(authorsTextField, c);
		row++;

		JLabel agendaTitleLabel = new JLabel();
		agendaTitleLabel.setFont(new java.awt.Font("SansSerif", 0, 11));
		agendaTitleLabel.setText("Agenda:"); // NOI18N
		c.gridx = 0;
		c.gridy = row;
		c.insets = insets;
		add(agendaTitleLabel, c);

		agendaTitleTextField = new JTextField(40);
		c.gridx = 1;
		c.gridy = row;
		c.weightx = 1.0;
		c.gridwidth = 2;
		add(agendaTitleTextField, c);
		row++;

		JLabel workItemLabel = new JLabel();
		workItemLabel.setFont(new java.awt.Font("SansSerif", 0, 11));
		workItemLabel.setText("Work Item:"); // NOI18N
		c.gridx = 0;
		c.gridy = row;
		c.insets = insets;
		add(workItemLabel, c);

		workItemTextField = new JTextField(40);
		c.gridx = 1;
		c.gridy = row;
		c.weightx = 1.0;
		c.gridwidth = 2;
		add(workItemTextField, c);
		row++;

		JLabel commentLabel = new JLabel();
		workItemLabel.setFont(new java.awt.Font("SansSerif", 0, 11));
		workItemLabel.setText("Comment:"); // NOI18N
		c.gridx = 0;
		c.gridy = row;
		c.insets = insets;
		add(commentLabel, c);

		commentTextField = new JTextField(40);
		c.gridx = 1;
		c.gridy = row;
		c.weightx = 1.0;
		c.gridwidth = 2;
		add(commentTextField, c);
		row++;

		JLabel notesLabel = new JLabel();
		notesLabel.setFont(new java.awt.Font("SansSerif", 0, 11)); // NOI18N
		notesLabel.setText("Notes:"); // NOI18N
		c.gridx = 0;
		c.gridy = row;
		c.insets = insets;
		add(notesLabel, c);

		notesTextField = new JTextField(40);
		c.gridx = 1;
		c.gridy = row;
		c.weightx = 1.0;
		c.gridwidth = 2;
		add(notesTextField, c);
		row++;

		JLabel decisionLabel = new JLabel();
		decisionLabel.setFont(new java.awt.Font("SansSerif", 0, 11)); // NOI18N
		decisionLabel.setText("Decision:"); // NOI18N
		c.gridx = 0;
		c.gridy = row;
		c.insets = insets;
		add(decisionLabel, c);

		decisionTextField = new JTextField(40);
		c.gridx = 1;
		c.gridy = row;
		c.weightx = 1.0;
		c.gridwidth = 2;
		add(decisionTextField, c);
		row++;

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		JButton clearButton = new JButton();
		clearButton.setFont(new java.awt.Font("SansSerif", 0, 11));
		clearButton.setText("Clear"); // NOI18N
		clearButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				clearButtonActionPerformed(evt);
			}
		});
		buttonPanel.add(clearButton);

		JButton cancelButton = new JButton();
		cancelButton.setFont(new java.awt.Font("SansSerif", 0, 11));
		cancelButton.setText("Cancel"); // NOI18N
		cancelButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cancelButtonActionPerformed(evt);
			}
		});
		buttonPanel.add(cancelButton);

		JButton okButton = new JButton();
		okButton.setFont(new java.awt.Font("SansSerif", 0, 11));
		okButton.setText("OK"); // NOI18N
		okButton.setMaximumSize(new java.awt.Dimension(1000, 23));
		okButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				okButtonActionPerformed(evt);
			}
		});
		buttonPanel.add(okButton);

		c.gridx = 0;
		c.gridy = row;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(buttonPanel, c);

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		pack();

	}

	private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
		filter();
		setVisible(false);
	}

	private void filter() {
		entries = DataAccessObject.getInstance().findEntries(parent.getTable(),
				"%" + parent.getMeeting() + "%",
				"%" + tDocTextField.getText() + "%",
				"%" + titleTextField.getText() + "%",
				"%" + authorsTextField.getText() + "%",
				"%" + notesTextField.getText() + "%",
				"%" + agendaTitleTextField.getText() + "%",
				"%" + workItemTextField.getText() + "%",
				"%" + decisionTextField.getText() + "%",
				"%" + commentTextField.getText() + "%");
	}

	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
		this.setVisible(false);
	}

	private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {
		tDocTextField.setText("");
		titleTextField.setText("");
		authorsTextField.setText("");
		agendaTitleTextField.setText("");
		notesTextField.setText("");
		workItemTextField.setText("");
		decisionTextField.setText("");
		commentTextField.setText("");
	}

}
