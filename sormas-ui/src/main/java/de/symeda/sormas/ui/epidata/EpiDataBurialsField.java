/*******************************************************************************
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package de.symeda.sormas.ui.epidata;

import java.util.function.Consumer;

import com.vaadin.ui.Table;
import com.vaadin.ui.Window;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.epidata.EpiDataBurialDto;
import de.symeda.sormas.api.location.LocationDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.ui.UserProvider;
import de.symeda.sormas.ui.caze.AbstractTableField;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent.CommitListener;
import de.symeda.sormas.ui.utils.CommitDiscardWrapperComponent.DeleteListener;
import de.symeda.sormas.ui.utils.VaadinUiUtil;

@SuppressWarnings("serial")
public class EpiDataBurialsField extends AbstractTableField<EpiDataBurialDto> {
	
	private static final String EPI_DATA_BURIAL_TABLE_PREFIX = "EpiDataBurialTable";
	
	private static final String PERIOD = "period";
	private static final String CITY = "city";
	private static final String DISTRICT = "district";
	
	@Override
	public Class<EpiDataBurialDto> getEntryType() {
		return EpiDataBurialDto.class;
	}
	
	@Override
	protected void updateColumns() {
		Table table = getTable();
		
		table.addGeneratedColumn(PERIOD, new Table.ColumnGenerator() {
			@Override
			public Object generateCell(Table source, Object itemId, Object columnId) {
				EpiDataBurialDto burial = (EpiDataBurialDto) itemId;
				if (burial.getBurialDateFrom() == null && burial.getBurialDateTo() == null) {
					return "Unknown";
				} else {
					StringBuilder periodBuilder = new StringBuilder();
					periodBuilder.append(burial.getBurialDateFrom() != null ? DateHelper.formatLocalDate(burial.getBurialDateFrom()) : "?");
					periodBuilder.append(" - ");
					periodBuilder.append(burial.getBurialDateTo() != null ? DateHelper.formatLocalDate(burial.getBurialDateTo()) : "?");
					return periodBuilder.toString();
				}
			}
		});
		
		table.addGeneratedColumn(CITY, new Table.ColumnGenerator() {
			@Override
			public Object generateCell(Table source, Object itemId, Object columnId) {
				EpiDataBurialDto burial = (EpiDataBurialDto) itemId;
				LocationDto location = burial.getBurialAddress();
				return location.getCity();
			}
		});
		
		table.addGeneratedColumn(DISTRICT, new Table.ColumnGenerator() {
			@Override
			public Object generateCell(Table source, Object itemId, Object columnId) {
				EpiDataBurialDto burial = (EpiDataBurialDto) itemId;
				LocationDto location = burial.getBurialAddress();
				return location.getDistrict();
			}
		});
		
		table.setVisibleColumns(
				EDIT_COLUMN_ID,
				EpiDataBurialDto.BURIAL_PERSON_NAME,
				EpiDataBurialDto.BURIAL_RELATION,
				PERIOD,
				CITY,
				DISTRICT,
				EpiDataBurialDto.BURIAL_ILL,
				EpiDataBurialDto.BURIAL_TOUCHING);

		table.setColumnExpandRatio(EDIT_COLUMN_ID, 0);
		table.setColumnExpandRatio(EpiDataBurialDto.BURIAL_PERSON_NAME, 0);
		table.setColumnExpandRatio(EpiDataBurialDto.BURIAL_RELATION, 0);
		table.setColumnExpandRatio(PERIOD, 0);
		table.setColumnExpandRatio(CITY, 0);
		table.setColumnExpandRatio(DISTRICT, 0);
		table.setColumnExpandRatio(EpiDataBurialDto.BURIAL_ILL, 0);
		table.setColumnExpandRatio(EpiDataBurialDto.BURIAL_TOUCHING, 0);
		
		for (Object columnId : table.getVisibleColumns()) {
			table.setColumnHeader(columnId, I18nProperties.getPrefixCaption(EPI_DATA_BURIAL_TABLE_PREFIX, (String) columnId));
		}
	}
	
	@Override
	protected boolean isEmpty(EpiDataBurialDto entry) {
		return false;
	}
	
	@Override
	protected boolean isModified(EpiDataBurialDto oldEntry, EpiDataBurialDto newEntry) {
		if (isModifiedObject(oldEntry.getBurialDateFrom(), newEntry.getBurialDateFrom()))
			return true;
		if (isModifiedObject(oldEntry.getBurialDateTo(), newEntry.getBurialDateTo()))
			return true;
		if (isModifiedObject(oldEntry.getBurialAddress(), newEntry.getBurialAddress()))
			return true;
		if (isModifiedObject(oldEntry.getBurialPersonName(), newEntry.getBurialPersonName()))
			return true;
		if (isModifiedObject(oldEntry.getBurialRelation(), newEntry.getBurialRelation()))
			return true;
		if (isModifiedObject(oldEntry.getBurialIll(), newEntry.getBurialIll()))
			return true;
		if (isModifiedObject(oldEntry.getBurialTouching(), newEntry.getBurialTouching()))
			return true;
		
		return false;
	}
	
	@Override
	protected void editEntry(EpiDataBurialDto entry, boolean create, Consumer<EpiDataBurialDto> commitCallback) {
		EpiDataBurialEditForm editForm = new EpiDataBurialEditForm(create, UserRight.CASE_EDIT);
		editForm.setValue(entry);
		
		final CommitDiscardWrapperComponent<EpiDataBurialEditForm> editView = new CommitDiscardWrapperComponent<EpiDataBurialEditForm>(editForm, editForm.getFieldGroup());
		editView.getCommitButton().setCaption("done");

		Window popupWindow = VaadinUiUtil.showModalPopupWindow(editView, "Burial");
		
		editView.addCommitListener(new CommitListener() {
			@Override
			public void onCommit() {
				if (!editForm.getFieldGroup().isModified()) {
					commitCallback.accept(editForm.getValue());
				}
			}
		});
		
		if (!isEmpty(entry)) {
			editView.addDeleteListener(new DeleteListener() {
				@Override
				public void onDelete() {
					popupWindow.close();
					EpiDataBurialsField.this.removeEntry(entry);
				}
			}, I18nProperties.getCaption("EpiDataBurial"));
		}
	}
	
	@Override
	protected EpiDataBurialDto createEntry() {
		EpiDataBurialDto burial = new EpiDataBurialDto();
		burial.setUuid(DataHelper.createUuid());
		LocationDto location = new LocationDto();
		location.setUuid(DataHelper.createUuid());
		location.setRegion(UserProvider.getCurrent().getUser().getRegion());
		burial.setBurialAddress(location);
		return burial;
	}

}
