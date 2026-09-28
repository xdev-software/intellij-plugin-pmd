package software.xdev.pmd.ui.config.project.components.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;


public abstract class LocationTableModel<T> extends AbstractTableModel
{
	protected final List<T> locations = new ArrayList<>();
	
	public void setLocations(final List<T> newLocations)
	{
		this.locations.clear();
		
		if(newLocations != null)
		{
			this.locations.addAll(newLocations);
		}
		
		this.fireTableDataChanged();
	}
	
	public void addLocation(final T location)
	{
		if(location != null)
		{
			this.locations.add(location);
			this.fireTableRowsInserted(this.locations.size() - 1, this.locations.size() - 1);
		}
	}
	
	public void removeLocationAt(final int index)
	{
		this.locations.remove(index);
		
		this.fireTableRowsDeleted(index, index);
	}
	
	public T getLocationAt(final int index)
	{
		return this.locations.get(index);
	}
	
	public List<T> getLocations()
	{
		return Collections.unmodifiableList(this.locations);
	}
}
