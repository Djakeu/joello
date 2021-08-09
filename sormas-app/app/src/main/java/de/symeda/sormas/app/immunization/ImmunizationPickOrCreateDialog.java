/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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
 */

package de.symeda.sormas.app.immunization;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableField;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

import de.symeda.sormas.app.BR;
import de.symeda.sormas.app.BaseActivity;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.common.DatabaseHelper;
import de.symeda.sormas.app.backend.immunization.Immunization;
import de.symeda.sormas.app.backend.immunization.ImmunizationCriteria;
import de.symeda.sormas.app.backend.immunization.ImmunizationSimilarityCriteria;
import de.symeda.sormas.app.component.dialog.AbstractDialog;
import de.symeda.sormas.app.core.IEntryItemOnClickListener;
import de.symeda.sormas.app.core.notification.NotificationHelper;
import de.symeda.sormas.app.core.notification.NotificationType;
import de.symeda.sormas.app.databinding.DialogImmunizationPickOrCreateLayoutBinding;
import de.symeda.sormas.app.util.Consumer;
import de.symeda.sormas.app.util.ViewHelper;

public class ImmunizationPickOrCreateDialog extends AbstractDialog {

	public static final String TAG = ImmunizationPickOrCreateDialog.class.getSimpleName();

	private DialogImmunizationPickOrCreateLayoutBinding contentBinding;

	private ImmunizationSimilarityCriteria criteria;
	private List<Immunization> similarImmunizations;

	private IEntryItemOnClickListener similarImmunizationItemClickCallback;
	private final ObservableField<Immunization> selectedImmunization = new ObservableField<>();

	// Static methods

	public static void pickOrCreateImmunization(final Immunization newImmunization, final Consumer<Immunization> pickedImmunizationCallback) {
		final ImmunizationPickOrCreateDialog dialog = new ImmunizationPickOrCreateDialog(BaseActivity.getActiveActivity(), newImmunization);

		if (!dialog.hasSimilarImmunizations()) {
			pickedImmunizationCallback.accept(newImmunization);
			return;
		}

		dialog.setPositiveCallback(() -> {
			pickedImmunizationCallback.accept(dialog.getSelectedImmunization() != null ? dialog.getSelectedImmunization() : newImmunization);
		});
		dialog.setNegativeCallback(() -> {
			pickedImmunizationCallback.accept(null);
		});

		dialog.show();
		dialog.getPositiveButton().setEnabled(false);
	}

	// Constructors

	private ImmunizationPickOrCreateDialog(final FragmentActivity activity, Immunization newImmunization) {
		super(
			activity,
			R.layout.dialog_root_layout,
			R.layout.dialog_immunization_pick_or_create_layout,
			R.layout.dialog_root_two_button_panel_layout,
			R.string.heading_pick_or_create_immunization,
			-1);

		ImmunizationCriteria immunizationCriteria = new ImmunizationCriteria();

		immunizationCriteria.setResponsibleRegion(newImmunization.getResponsibleRegion());
		immunizationCriteria.setDisease(newImmunization.getDisease());
		this.criteria = new ImmunizationSimilarityCriteria();
		this.criteria.setImmunizationCriteria(immunizationCriteria);
		this.criteria.setPersonUuid(newImmunization.getPerson().getUuid());
		this.criteria.setImmunizationUuid(newImmunization.getUuid());
		this.criteria.setReportDate(newImmunization.getReportDate());
		this.criteria.setStartDate(newImmunization.getStartDate());
		this.criteria.setEndDate(newImmunization.getEndDate());

		this.setSelectedImmunization(null);
	}

	// Instance methods

	private boolean hasSimilarImmunizations() {
		updateSimilarImmunizations();
		return !similarImmunizations.isEmpty();
	}

	private void updateSimilarImmunizations() {
		similarImmunizations = DatabaseHelper.getImmunizationDao().getSimilarImmunizations(criteria);
	}

	private void setupControlListeners() {
		similarImmunizationItemClickCallback = (v, item) -> {
			if (item == null) {
				return;
			}

			Immunization immunizationItem = (Immunization) item;
			String tag = getActivity().getResources().getString(R.string.tag_row_item_immunization_pick_or_create);
			ArrayList<View> views = ViewHelper.getViewsByTag(contentBinding.existingImmunizationsList, tag);
			setSelectedImmunization(null);

			for (View itemView : views) {
				try {
					int itemViewId = itemView.getId();
					int vId = v.getId();

					if (itemViewId == vId && v.isSelected()) {
						itemView.setSelected(false);
					} else if (itemViewId == vId && !v.isSelected()) {
						itemView.setSelected(true);
						setSelectedImmunization(immunizationItem);
					} else {
						itemView.setSelected(false);
					}
				} catch (NumberFormatException ex) {
					NotificationHelper
						.showDialogNotification(ImmunizationPickOrCreateDialog.this, NotificationType.ERROR, R.string.error_internal_error);
				}
			}

			if (getSelectedImmunization() != null) {
				getPositiveButton().setEnabled(true);
			} else {
				getPositiveButton().setEnabled(false);
			}
		};
	}

	private ObservableArrayList makeObservable(List<Immunization> immunizations) {
		ObservableArrayList<Immunization> newList = new ObservableArrayList<>();
		newList.addAll(immunizations);
		return newList;
	}

	// Overrides

	@Override
	protected void setContentBinding(Context context, ViewDataBinding binding, String layoutName) {
		this.contentBinding = (DialogImmunizationPickOrCreateLayoutBinding) binding;

		// Needs to be done here because callback needs to be initialized before being bound to the layout
		setupControlListeners();

		if (!binding.setVariable(BR.similarImmunizations, makeObservable(similarImmunizations))) {
			Log.e(TAG, "There is no variable 'similarImmunizations' in layout " + layoutName);
		}

		if (!binding.setVariable(BR.similarImmunizationItemClickCallback, similarImmunizationItemClickCallback)) {
			Log.e(TAG, "There is no variable 'similarImmunizationItemClickCallback' in layout " + layoutName);
		}
	}

	@Override
	protected void initializeContentView(ViewDataBinding rootBinding, final ViewDataBinding buttonPanelBinding) {
		this.selectedImmunization.notifyChange();
	}

	@Override
	public int getPositiveButtonText() {
		return R.string.action_confirm;
	}

	@Override
	public int getNegativeButtonText() {
		return R.string.action_dismiss;
	}

	// Getters & setters

	private Immunization getSelectedImmunization() {
		return selectedImmunization.get();
	}

	private void setSelectedImmunization(Immunization selectedImmunization) {
		this.selectedImmunization.set(selectedImmunization);
	}
}
