package software.xdev.pmd.ui.config.project.components.thirdpartyclasspath;

import java.util.Collections;

import software.xdev.pmd.model.config.thirdpartycplocation.ThirdPartyCPLocation;
import software.xdev.pmd.ui.config.project.components.shared.LocationTableModel;


public class TPCPLocationTableModel extends LocationTableModel<ThirdPartyCPLocation>
{
	private static final int COLUMN_TYPE = 0;
	private static final int COLUMN_LOCATION = 1;
	private static final int NUMBER_OF_COLUMNS = 2;
	
	public boolean trySwap(final int index, final int otherIndex)
	{
		try
		{
			Collections.swap(this.locations, index, otherIndex);
			return true;
		}
		catch(final IndexOutOfBoundsException ignored)
		{
			return false;
		}
	}
	
	@Override
	public int getColumnCount()
	{
		return NUMBER_OF_COLUMNS;
	}
	
	@Override
	public Class<?> getColumnClass(final int columnIndex)
	{
		return String.class;
	}
	
	@Override
	public String getColumnName(final int column)
	{
		return switch(column)
		{
			case COLUMN_TYPE -> "Type";
			case COLUMN_LOCATION -> "Location";
			default -> "???";
		};
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
			case COLUMN_TYPE -> this.locations.get(rowIndex).type().name();
			case COLUMN_LOCATION -> this.locations.get(rowIndex).displayLocation();
			default -> throw new IllegalArgumentException("Invalid column: " + columnIndex);
		};
	}
}
