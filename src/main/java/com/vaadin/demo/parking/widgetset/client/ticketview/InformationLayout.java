package com.vaadin.demo.parking.widgetset.client.ticketview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import com.google.gwt.core.client.Callback;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.geolocation.client.Geolocation;
import com.google.gwt.geolocation.client.PositionError;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.addon.touchkit.gwt.client.ui.DatePicker;
import com.vaadin.addon.touchkit.gwt.client.ui.DatePicker.Resolution;
import com.vaadin.addon.touchkit.gwt.client.ui.VSwitch;
import com.vaadin.addon.touchkit.gwt.client.ui.VerticalComponentGroupWidget;
import com.vaadin.client.ui.VTextField;
import com.vaadin.demo.parking.widgetset.client.model.Location;
import com.vaadin.demo.parking.widgetset.client.model.Ticket;
import com.vaadin.demo.parking.widgetset.client.model.Violation;

public class InformationLayout extends VerticalComponentGroupWidget {
    private final VSwitch useCurrentLocationSwitch;
    private com.google.gwt.geolocation.client.Position currentPosition;
    private final VTextField addressField;
    private final Widget addressRow;
    private final DatePicker timeField;
    private Date date;
    private final VTextField vehicleIdField;
    private final ListBox violationBox;
    private final ListBox areaBox;
    private final TicketViewModuleListener listener;

    private void requestUserPosition() {
        Geolocation
                .getIfSupported()
                .getCurrentPosition(
                        new Callback<com.google.gwt.geolocation.client.Position, PositionError>() {
                            @Override
                            public void onSuccess(
                                    final com.google.gwt.geolocation.client.Position result) {
                                currentPosition = result;
                                if (listener != null) {
                                    listener.positionReceived(result
                                            .getCoordinates().getLatitude(),
                                            result.getCoordinates()
                                                    .getLongitude());
                                }
                                setUseCurrentPositionEnabled(true);
                            }

                            @Override
                            public void onFailure(final PositionError reason) {
                                setUseCurrentPositionEnabled(false);
                            }
                        });
    }

    private void setUseCurrentPositionEnabled(final boolean enabled) {
        useCurrentLocationSwitch.setValue(enabled);
        addressRow.getElement().getParentElement().getParentElement()
                .getStyle()
                .setProperty("display", enabled ? "none" : "-webkit-box");
    }

    private Element getRowElement(final Widget field) {
        Element elem = field.getElement();
        while (!elem.getClassName().contains("v-touchkit-componentgroup-row")) {
            elem = elem.getParentElement();
        }
        return elem;
    }

    public final boolean validateFields() {
        resetValidations();

        ArrayList<Widget> invalidFields = new ArrayList<Widget>();

        boolean valid = true;
        if (!useCurrentLocationSwitch.getValue()
                && (addressField.getText() == null || addressField.getText()
                        .trim().isEmpty())) {
            valid = false;
            invalidFields.add(addressField);
        }
        if (date == null) {
            valid = false;
            timeField.add(vehicleIdField);
        }
        if (vehicleIdField.getText() == null
                || vehicleIdField.getText().trim().isEmpty()) {
            valid = false;
            invalidFields.add(vehicleIdField);
        }
        if ("null"
                .equals(violationBox.getValue(violationBox.getSelectedIndex()))) {
            valid = false;
            invalidFields.add(violationBox);
        }
        if ("null".equals(areaBox.getValue(areaBox.getSelectedIndex()))) {
            valid = false;
            invalidFields.add(areaBox);
        }
        for (Widget invalidField : invalidFields) {
            getRowElement(invalidField).addClassName("invalid");
        }
        return valid;
    }

    public InformationLayout(final TicketViewModuleListener listener) {
        this.listener = listener;

        useCurrentLocationSwitch = new VSwitch();
        useCurrentLocationSwitch
                .addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                    @Override
                    public void onValueChange(
                            final ValueChangeEvent<Boolean> event) {
                        if (event.getValue()) {
                            requestUserPosition();
                        } else {
                            setUseCurrentPositionEnabled(false);
                        }
                    }
                });
        add(buildFieldRowBox("Current location", useCurrentLocationSwitch));

        final ValueChangeHandler<String> vch = new ValueChangeHandler<String>() {
            @Override
            public void onValueChange(final ValueChangeEvent<String> event) {
                listener.fieldsChanged();
            }
        };

        addressField = new VTextField();
        addressField.addValueChangeHandler(vch);
        addressField.getElement().getStyle().setProperty("width", "auto");
        addressRow = buildFieldRowBox("Address", addressField);
        add(addressRow);

        timeField = new DatePicker();
        timeField.setResolution(Resolution.TIME);
        timeField.addValueChangeHandler(new ValueChangeHandler<Date>() {
            @Override
            public void onValueChange(final ValueChangeEvent<Date> event) {
                date = event.getValue();
                listener.fieldsChanged();
            }
        });
        add(buildFieldRowBox("Time", timeField));

        vehicleIdField = new VTextField();
        vehicleIdField.addValueChangeHandler(vch);
        add(buildFieldRowBox("Vehicle ID", vehicleIdField));

        final ChangeHandler ch = new ChangeHandler() {
            @Override
            public void onChange(final ChangeEvent event) {
                listener.fieldsChanged();
            }
        };

        violationBox = new ListBox();
        violationBox.addChangeHandler(ch);
        violationBox.addItem("Choose...", (String) null);
        for (Violation violation : Violation.values()) {
            violationBox.addItem(violation.getCaption(), violation.name());
        }
        add(buildFieldRowBox("Violation", violationBox));

        areaBox = new ListBox();
        areaBox.addChangeHandler(ch);
        areaBox.addItem("Choose...", (String) null);
        for (char zone : "ABC".toCharArray()) {
            for (int i = 1; i < 5; i++) {
                String area = String.valueOf(zone) + i;
                areaBox.addItem(area, area);
            }
        }
        add(buildFieldRowBox("Area", areaBox));

        setUseCurrentPositionEnabled(true);
        requestUserPosition();
    }

    private Widget buildFieldRowBox(final String title, final Widget widget) {
        CaptionComponentFlexBox fb = new CaptionComponentFlexBox();
        Label label = new Label(title);
        label.setWidth("40%");
        fb.add(label);
        widget.setWidth("60%");
        fb.add(widget);
        return fb;
    }

    public final void resetValidations() {
        for (Widget field : Arrays.asList(addressField, timeField,
                vehicleIdField, violationBox, areaBox)) {
            getRowElement(field).removeClassName("invalid");
        }
    }

    public final void populateTicket(final Ticket ticket) {
        final Location location = new Location();
        if (!addressRow.isVisible() && currentPosition != null) {
            location.setLatitude(currentPosition.getCoordinates().getLatitude());
            location.setLongitude(currentPosition.getCoordinates()
                    .getLongitude());
        }
        location.setAddress(addressField.getText());
        ticket.setLocation(location);

        ticket.setTimeStamp(date);

        ticket.setRegisterPlateNumber(vehicleIdField.getText());

        String violationString = violationBox.getValue(violationBox
                .getSelectedIndex());
        ticket.setViolation("null".equals(violationString) ? null : Violation
                .valueOf(violationString));

        ticket.setArea(areaBox.getValue(areaBox.getSelectedIndex()));
    }

    public final void populateModule(final Ticket ticket) {
        addressField.setText(ticket.getLocation().getAddress());

        vehicleIdField.setText(ticket.getRegisterPlateNumber());

        violationBox.setSelectedIndex(0);
        for (int i = 0; i < violationBox.getItemCount(); i++) {
            if (ticket.getViolation() != null
                    && violationBox.getValue(i).equals(
                            ticket.getViolation().name())) {
                violationBox.setSelectedIndex(i);
                break;
            }
        }

        areaBox.setSelectedIndex(0);
        for (int i = 0; i < areaBox.getItemCount(); i++) {
            if (areaBox.getValue(i).equals(ticket.getArea())) {
                areaBox.setSelectedIndex(i);
                break;
            }
        }

        date = ticket.getTimeStamp();
        timeField.setDate(date);
    }

}
