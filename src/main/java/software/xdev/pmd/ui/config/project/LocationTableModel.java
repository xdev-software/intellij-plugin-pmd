package software.xdev.pmd.ui.config.project;

import static java.util.function.Predicate.not;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.table.AbstractTableModel;

import org.jetbrains.annotations.NotNull;

import com.intellij.psi.search.scope.packageSet.NamedScope;

import software.xdev.pmd.model.config.ConfigurationLocation;


/**
 * A table model for editing CheckStyle file locations.
 */
public class LocationTableModel extends AbstractTableModel
{
	private static final int COLUMN_ACTIVE = 0;
	private static final int COLUMN_DESCRIPTION = 1;
	private static final int COLUMN_FILE = 2;
	private static final int COLUMN_SCOPE = 3;
	private static final int NUMBER_OF_COLUMNS = 4;
	
	private final List<ConfigurationLocation> locations = new ArrayList<>();
	private final SortedSet<ConfigurationLocation> activeLocations = new TreeSet<>();
	
	public void setLocations(final List<ConfigurationLocation> newLocations)
	{
		this.locations.clear();
		
		if(newLocations != null)
		{
			this.locations.addAll(newLocations);
		}
		
		this.activeLocations.removeIf(not(this.locations::contains));
		
		this.fireTableDataChanged();
	}
	
	public void addLocation(final ConfigurationLocation location)
	{
		if(location != null)
		{
			this.locations.add(location);
			this.fireTableRowsInserted(this.locations.size() - 1, this.locations.size() - 1);
		}
	}
	
	public void updateLocation(final ConfigurationLocation location, final ConfigurationLocation newLocation)
	{
		if(location != null && newLocation != null)
		{
			final int index = this.locations.indexOf(location);
			if(index != -1)
			{
				this.locations.remove(index);
				this.locations.add(index, newLocation);
				this.fireTableRowsUpdated(index, index);
			}
		}
	}
	
	public void removeLocationAt(final int index)
	{
		final ConfigurationLocation locationToRemove = this.locations.get(index);
		if(this.activeLocations.contains(locationToRemove))
		{
			final TreeSet<ConfigurationLocation> newActiveLocations = new TreeSet<>(this.activeLocations);
			newActiveLocations.remove(locationToRemove);
			this.setActiveLocations(newActiveLocations);
		}
		this.locations.remove(index);
		
		this.fireTableRowsDeleted(index, index);
	}
	
	public ConfigurationLocation getLocationAt(final int index)
	{
		return this.locations.get(index);
	}
	
	public void setActiveLocations(@NotNull final SortedSet<ConfigurationLocation> activeLocations)
	{
		if(!activeLocations.isEmpty() && !new HashSet<>(this.locations).containsAll(activeLocations))
		{
			throw new IllegalArgumentException("Active location is not in location list");
		}
		
		if(!activeLocations.isEmpty())
		{
			activeLocations.forEach(activeLocation -> this.updateActiveLocation(
				activeLocation,
				this.locations.indexOf(activeLocation), false));
		}
		else
		{
			this.activeLocations.clear();
		}
	}
	
	private void updateActiveLocation(
		@NotNull final ConfigurationLocation newLocation,
		final int newRow,
		final boolean allowToggle)
	{
		if(allowToggle && this.activeLocations.contains(newLocation))
		{
			this.activeLocations.remove(newLocation);
		}
		else
		{
			this.activeLocations.add(newLocation);
		}
		
		if(newRow >= 0)
		{
			this.fireTableCellUpdated(newRow, COLUMN_ACTIVE);
		}
	}
	
	public SortedSet<ConfigurationLocation> getActiveLocations()
	{
		return this.activeLocations;
	}
	
	/**
	 * Clear all data from this table model.
	 */
	public void clear()
	{
		this.locations.clear();
		
		this.fireTableDataChanged();
	}
	
	public List<ConfigurationLocation> getLocations()
	{
		return Collections.unmodifiableList(this.locations);
	}
	
	@Override
	public int getColumnCount()
	{
		return NUMBER_OF_COLUMNS;
	}
	
	@Override
	public Class<?> getColumnClass(final int columnIndex)
	{
		if(columnIndex == COLUMN_ACTIVE)
		{
			return Boolean.class;
		}
		else
		{
			return String.class;
		}
	}
	
	@Override
	public String getColumnName(final int column)
	{
		return switch(column)
		{
			case COLUMN_ACTIVE -> "Active";
			case COLUMN_DESCRIPTION -> "Description";
			case COLUMN_FILE -> "File";
			case COLUMN_SCOPE -> "Scope";
			default -> "???";
		};
	}
	
	@Override
	public boolean isCellEditable(final int rowIndex, final int columnIndex)
	{
		return columnIndex == COLUMN_ACTIVE;
	}
	
	@Override
	public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex)
	{
		final ConfigurationLocation rowLocation = this.locations.get(rowIndex);
		if(columnIndex == COLUMN_ACTIVE)
		{
			this.updateActiveLocation(rowLocation, rowIndex, true);
			return;
		}
		throw new IllegalArgumentException("Column is not editable: " + columnIndex);
	}
	
	@Override
	public int getRowCount()
	{
		return this.locations.size();
	}
	
	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex)
	{
		return switch(columnIndex)
		{
			case COLUMN_ACTIVE -> this.activeLocations.contains(this.locations.get(rowIndex));
			case COLUMN_DESCRIPTION -> this.locations.get(rowIndex).getDescription();
			case COLUMN_FILE -> this.locations.get(rowIndex).getLocation();
			case COLUMN_SCOPE -> this.locations.get(rowIndex).getNamedScope()
				.map(NamedScope::getPresentableName)
				.orElse("");
			default -> throw new IllegalArgumentException("Invalid column: " + columnIndex);
		};
	}
}
