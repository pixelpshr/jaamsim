/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2004-2011 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.ui;

import java.awt.Dimension;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.sandwell.JavaSimulation.Entity;

/**
 * Class to display information about model objects. <br>
 * displays the results of getInfo for an object
 */
public class PropertyBox extends FrameBox {
	private static PropertyBox myInstance;  // only one instance allowed to be open
	private Entity currentEntity;
	private final JTabbedPane jTabbedFrame = new JTabbedPane();

	public PropertyBox() {
		super("Property Viewer");
		setDefaultCloseOperation(FrameBox.DISPOSE_ON_CLOSE);

		jTabbedFrame.setPreferredSize(new Dimension(800, 400));
		jTabbedFrame.addChangeListener(new TabListener());
		getContentPane().add(jTabbedFrame);

		setSize( 300, 150 );
		setLocation(0, 110);

		pack();
	}

	@Override
	public void setEntity(Entity entity) {
		if(currentEntity == entity)
			return;

		currentEntity = entity;
		jTabbedFrame.removeAll();
		if (currentEntity == null) {
			setTitle("Property Viewer");
			return;
		}

		setTitle("Property Viewer - " + currentEntity.getInputName());

		ArrayList<ClassFields> cFields = getFields(entity);
		for (int i = 0; i < cFields.size(); i++) {
			// The properties in the current page
			ClassFields cf = cFields.get(i);
			PropertyTableModel mod = new PropertyTableModel(entity, cf);
			PropertyTable tab = new PropertyTable(mod);
			JScrollPane scroll = new JScrollPane(tab);

			jTabbedFrame.addTab(cf.klass.getSimpleName(), scroll);
		}
	}

	@Override
	public void updateValues(double simTime) {
		if(currentEntity == null)
			return;

		JTable propTable = (JTable)(((JScrollPane)jTabbedFrame.getSelectedComponent()).getViewport().getComponent(0));
		((PropertyTableModel)propTable.getModel()).fireTableDataChanged();
	}

	/**
	 * Returns the only instance of the property box
	 */
	public synchronized static PropertyBox getInstance() {
		if (myInstance == null)
			myInstance = new PropertyBox();

		return myInstance;
	}

	private synchronized static void killInstance() {
		myInstance = null;
	}

	@Override
	public void dispose() {
		killInstance();
		super.dispose();
	}

private static class TabListener implements ChangeListener {
	@Override
	public void stateChanged(ChangeEvent e) {
		FrameBox.valueUpdate();
	}
}

private static class ClassFields implements Comparator<Field> {
	final Class<?> klass;
	final ArrayList<Field> fields;

	ClassFields(Class<?> aKlass) {
		klass = aKlass;

		Field[] myFields = klass.getDeclaredFields();
		fields = new ArrayList<Field>(myFields.length);
		for (Field each : myFields) {
			String name = each.getName();
			// Static variables are all capitalized (ignore them)
			if (name.toUpperCase().equals(name))
				continue;

			fields.add(each);
		}
		Collections.sort(fields, this);
	}

	@Override
	public int compare(Field f1, Field f2) {
		return f1.getName().compareToIgnoreCase(f2.getName());
	}
}

	private static ArrayList<ClassFields> getFields(Entity object) {
		if (object == null)
			return new ArrayList<ClassFields>(0);

		return getFields(object.getClass());
	}

	private static ArrayList<ClassFields> getFields(Class<?> klass) {
		if (klass == null || klass.getSuperclass() == null)
			return new ArrayList<ClassFields>();

		ArrayList<ClassFields> cFields = getFields(klass.getSuperclass());
		cFields.add(new ClassFields(klass));
		return cFields;
	}

	static String format(Object value) {
		if (value == null)
			return "<null>";

		if (value instanceof Entity)
			return ((Entity)value).getInputName();

		if (value instanceof double[])
			return Arrays.toString((double[])value);

		try {
			return value.toString();
		}
		catch (ConcurrentModificationException e) {
			return "";
		}
	}

private static class PropertyTable extends JTable {
	PropertyTable(TableModel model) {
		super(model);

		TableColumn col;

		col = getColumnModel().getColumn(0);
		col.setHeaderValue("Property");
		col.setWidth(150);
		col.setCellRenderer(FrameBox.colRenderer);

		col = getColumnModel().getColumn(1);
		col.setHeaderValue("Type");
		col.setWidth(150);
		col.setCellRenderer(FrameBox.colRenderer);

		col = getColumnModel().getColumn(2);
		col.setHeaderValue("Value");
		col.setWidth(150);
		col.setCellRenderer(FrameBox.colRenderer);

		getTableHeader().setFont(FrameBox.boldFont);
		getTableHeader().setReorderingAllowed(false);
	}

	@Override
	public void doLayout() {
		FrameBox.fitTableToLastColumn(this);
	}
}

private static class PropertyTableModel extends AbstractTableModel {
	Entity ent;
	ClassFields fields;

	PropertyTableModel(Entity e, ClassFields cf) {
		ent = e;
		fields = cf;
	}

	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public int getRowCount() {
		return fields.fields.size();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public Object getValueAt(int row, int col) {
		Field field = fields.fields.get(row);
		if (col == 0)
			return field.getName();

		if (col == 1)
			return field.getType().getSimpleName();

		try {
			// save the field's accessibility, reset after reading
			boolean accessible = field.isAccessible();
			field.setAccessible(true);

			Object o = field.get(ent);
			field.setAccessible(accessible);
			return format(o);
		}
		catch (SecurityException e) {}
		catch (IllegalArgumentException e) {}
		catch (IllegalAccessException e) {}

		return "";
	}
}
}
