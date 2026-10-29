import Shared
import SwiftUI
import UIKit

struct FiltersView: View {
    let model: ListingsModel

    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        if let editing = model.editing {
            content(editing)
                .background(Color.screenBackground)
                .sheet(isPresented: pendingLocationBinding) {
                    LocationRequestSheet(
                        onAllow: { Task { await model.allowLocation() } },
                        onDismiss: model.dismissLocationRequest
                    )
                    .presentationDetents([.medium, .large])
                }
                .onChange(of: scenePhase) { _, phase in
                    if phase == .active { model.refreshLocationState() }
                }
        }
    }

    private var pendingLocationBinding: Binding<Bool> {
        Binding(
            get: { model.editing?.pendingDistanceKm != nil },
            set: { if !$0 { model.dismissLocationRequest() } }
        )
    }

    private func content(_ editing: FiltersEditing) -> some View {
        let filters = editing.filters
        return ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                TopBar { close() }
                Section(title: "Prezzo") {
                    Stepper(label: "Da", value: filters.minPriceText) { up in model.edit { $0.stepMinPrice(up: up) } }
                    Stepper(label: "A", value: filters.maxPriceText) { up in model.edit { $0.stepMaxPrice(up: up) } }
                }
                DistanceSection(area: filters.area, locationDenied: editing.locationDenied, onChoose: model.chooseArea)
                Section(title: "Anno e chilometri") {
                    Stepper(label: "Immatricolato dal", value: String(filters.minYear)) { up in model.edit { $0.stepMinYear(up: up) } }
                    Stepper(label: "Chilometri al massimo", value: filters.maxMileageText) { up in model.edit { $0.stepMaxMileage(up: up) } }
                }
                TypesSection(selected: filters.camperTypes) { type in model.edit { $0.toggleCamperType(type: type) } }
                Button("Mostra i camper") { close() }
                    .buttonStyle(ActionButtonStyle())
                    .frame(maxWidth: .infinity)
                Button("Ripristina i valori iniziali", action: model.resetFilters)
                    .scaledFont(18, weight: .medium)
                    .foregroundStyle(Color.actionBlue)
                    .frame(maxWidth: .infinity, minHeight: 48)
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
    }

    private func close() {
        Task { await model.closeFilters() }
    }
}

private struct TopBar: View {
    let onClose: () -> Void

    var body: some View {
        HStack(spacing: 4) {
            Button(action: onClose) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 24, weight: .semibold))
                    .frame(width: 48, height: 48)
            }
            .foregroundStyle(.primary)
            .accessibilityLabel("Torna alla lista")
            Text("Filtri")
                .scaledFont(30, weight: .bold, relativeTo: .largeTitle)
        }
        .padding(.top, 8)
    }
}

private struct Section<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(title).scaledFont(21, weight: .bold)
            content
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.cardBackground, in: RoundedRectangle(cornerRadius: 20))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(Color.hairline))
    }
}

private struct Stepper: View {
    let label: String
    let value: String
    let onStep: (Bool) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label).scaledFont(18).foregroundStyle(Color.secondaryText)
            HStack(spacing: 10) {
                StepButton(symbol: "minus", description: "Diminuisci \(label)") { onStep(false) }
                Text(value)
                    .scaledFont(22, weight: .bold)
                    .frame(maxWidth: .infinity)
                StepButton(symbol: "plus", description: "Aumenta \(label)") { onStep(true) }
            }
        }
    }
}

private struct StepButton: View {
    let symbol: String
    let description: String
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Image(systemName: symbol)
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(Color.actionBlue)
                .frame(width: 52, height: 52)
                .overlay(Circle().stroke(Color.actionBlue, lineWidth: 2))
        }
        .accessibilityLabel(description)
    }
}

private struct DistanceSection: View {
    let area: SearchArea
    let locationDenied: Bool
    let onChoose: (SearchArea) -> Void

    @Environment(\.openURL) private var openURL

    var body: some View {
        Section(title: "Distanza da qui") {
            if locationDenied {
                Label("La posizione del telefono non è attiva, quindi mostriamo i camper delle regioni impostate.", systemImage: "exclamationmark.circle")
                    .scaledFont(17)
                    .foregroundStyle(Color.warning)
                    .padding(12)
                    .background(Color.warning.opacity(0.12), in: RoundedRectangle(cornerRadius: 14))
                Button("Attiva la posizione", action: openSettings)
                    .scaledFont(18, weight: .bold)
                    .foregroundStyle(Color.actionBlue)
                    .frame(maxWidth: .infinity, minHeight: 52)
                    .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.actionBlue, lineWidth: 2))
            } else {
                Text("Misurata dalla posizione del telefono")
                    .scaledFont(17)
                    .foregroundStyle(Color.secondaryText)
            }
            FlowLayout(spacing: 10) {
                ForEach(SearchFilters.companion.distanceOptionsKm, id: \.self) { km in
                    let nearby = SearchAreaNearby(km: km.int32Value)
                    AreaChip(label: "\(km) km", selected: isSame(area, nearby), enabled: !locationDenied) { onChoose(nearby) }
                }
                AreaChip(label: "Tutta Italia", selected: isSame(area, SearchAreaWholeItaly.shared), enabled: true) {
                    onChoose(SearchAreaWholeItaly.shared)
                }
            }
        }
    }

    private func isSame(_ first: SearchArea, _ second: SearchArea) -> Bool {
        (first as AnyObject).isEqual(second)
    }

    private func openSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        openURL(url)
    }
}

private struct AreaChip: View {
    let label: String
    let selected: Bool
    let enabled: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .scaledFont(18, weight: .medium)
                .foregroundStyle(selected ? .white : .primary)
                .padding(.horizontal, 16)
                .frame(minHeight: 48)
                .background(selected ? Color.actionBlue : Color.cardBackground, in: Capsule())
                .overlay(Capsule().stroke(selected ? Color.actionBlue : Color.chipBorder, lineWidth: 2))
        }
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.45)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

private struct TypesSection: View {
    let selected: [String]
    let onToggle: (String) -> Void

    var body: some View {
        Section(title: "Tipologie") {
            VStack(spacing: 0) {
                ForEach(Array(SearchFilters.companion.camperTypeOptions.enumerated()), id: \.element) { index, type in
                    if index > 0 { Divider().overlay(Color.hairline) }
                    let isOn = selected.contains(type)
                    Button { onToggle(type) } label: {
                        HStack(spacing: 14) {
                            Image(systemName: isOn ? "checkmark.square.fill" : "square")
                                .font(.system(size: 28))
                                .foregroundStyle(isOn ? Color.actionBlue : Color.secondaryText)
                            Text(type.prefix(1).uppercased() + type.dropFirst())
                                .scaledFont(19)
                                .foregroundStyle(.primary)
                            Spacer()
                        }
                        .frame(minHeight: 56)
                        .contentShape(Rectangle())
                    }
                    .accessibilityAddTraits(isOn ? .isSelected : [])
                }
            }
        }
    }
}

private struct LocationRequestSheet: View {
    let onAllow: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "location")
                .font(.system(size: 40, weight: .medium))
                .foregroundStyle(Color.sage)
                .frame(width: 88, height: 88)
                .background(Color.sageContainer, in: Circle())
                .accessibilityHidden(true)
            Text("Cerchiamo i camper vicini a voi?")
                .scaledFont(24, weight: .bold)
            Text("Per sapere quanto è lontano ogni camper, CamperTe ha bisogno della posizione del telefono. La usa solo per calcolare la distanza.")
                .scaledFont(18)
                .foregroundStyle(Color.secondaryText)
            Button("Usa la mia posizione", action: onAllow)
                .buttonStyle(ActionButtonStyle())
            Button("Non ora", action: onDismiss)
                .scaledFont(18, weight: .medium)
                .foregroundStyle(Color.actionBlue)
                .frame(minHeight: 48)
        }
        .multilineTextAlignment(.center)
        .padding(24)
    }
}
