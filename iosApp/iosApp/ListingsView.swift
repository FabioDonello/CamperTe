import Shared
import SwiftUI

struct ListingsView: View {
    let model: ListingsModel

    @Environment(\.scenePhase) private var scenePhase
    @State private var wasInBackground = false

    var body: some View {
        content
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(Color.screenBackground)
            .task { await model.load() }
            .fullScreenCover(isPresented: filtersBinding) {
                FiltersView(model: model)
            }
            .onChange(of: scenePhase) { _, phase in
                if phase == .background {
                    wasInBackground = true
                } else if phase == .active && wasInBackground {
                    wasInBackground = false
                    Task { await model.load() }
                }
            }
    }

    private var filtersBinding: Binding<Bool> {
        Binding(
            get: { model.editing != nil },
            set: { if !$0 { Task { await model.closeFilters() } } }
        )
    }

    @ViewBuilder
    private var content: some View {
        if let result = model.result {
            switch onEnum(of: result) {
            case .found(let found):
                FoundView(found: found, filtersSummary: model.filtersSummary, onRefresh: model.load, onOpenFilters: model.openFilters)
            case .noConnection:
                NoConnectionView { Task { await model.retry() } }
            }
        } else {
            LoadingView()
        }
    }
}

private struct ScreenTitle: View {
    var body: some View {
        Text("I camper per voi")
            .scaledFont(30, weight: .bold, relativeTo: .largeTitle)
            .foregroundStyle(.primary)
    }
}

private struct CenteredState<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ScreenTitle()
                .padding(.horizontal, 20)
                .padding(.top, 24)
            VStack(spacing: 20) {
                content
            }
            .multilineTextAlignment(.center)
            .padding(.horizontal, 32)
            .padding(.bottom, 96)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}

private struct LoadingView: View {
    var body: some View {
        CenteredState {
            ProgressView()
                .controlSize(.large)
                .tint(.sage)
            Text("Sto cercando i camper per voi…")
                .scaledFont(20)
        }
    }
}

private struct NoConnectionView: View {
    let onRetry: () -> Void

    var body: some View {
        CenteredState {
            RoundIcon(systemName: "wifi.slash", background: .pill, tint: .secondaryText)
            Text("Nessuna connessione.\nRiprovate tra poco.")
                .scaledFont(22, weight: .medium)
            Button(action: onRetry) {
                Label("Riprova", systemImage: "arrow.clockwise")
            }
            .buttonStyle(ActionButtonStyle())
            .padding(.top, 8)
        }
    }
}

private struct RoundIcon: View {
    let systemName: String
    let background: Color
    let tint: Color

    var body: some View {
        Image(systemName: systemName)
            .font(.system(size: 40, weight: .medium))
            .foregroundStyle(tint)
            .frame(width: 96, height: 96)
            .background(background, in: Circle())
            .accessibilityHidden(true)
    }
}

private struct FoundView: View {
    let found: SearchResultFound
    let filtersSummary: String
    let onRefresh: () async -> Void
    let onOpenFilters: () -> Void

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 20) {
                Header(found: found, filtersSummary: filtersSummary, onOpenFilters: onOpenFilters)
                    .padding(.horizontal, 20)
                if found.listings.isEmpty {
                    EmptyResultsView()
                        .containerRelativeFrame(.vertical) { height, _ in height * 0.7 }
                }
                ForEach(found.listings, id: \.listing.id) { shown in
                    ListingCardView(shown: shown)
                        .padding(.horizontal, 12)
                }
            }
            .padding(.top, 24)
            .padding(.bottom, 24)
        }
        .refreshable { await onRefresh() }
        .overlay(alignment: .top) {
            Color.clear
                .frame(height: 0)
                .background(Color.screenBackground.opacity(0.92), ignoresSafeAreaEdges: .top)
        }
    }
}

private struct Header: View {
    let found: SearchResultFound
    let filtersSummary: String
    let onOpenFilters: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                ScreenTitle()
                Spacer(minLength: 0)
                Button(action: onOpenFilters) {
                    Label("Filtri", systemImage: "slider.horizontal.3")
                }
                .buttonStyle(ActionButtonStyle(horizontalPadding: 16, minHeight: 48, fontSize: 18))
            }
            Button(action: onOpenFilters) {
                Text(filtersSummary)
                    .scaledFont(17)
                    .foregroundStyle(Color.secondaryText)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(Color.pill, in: RoundedRectangle(cornerRadius: 14))
            }
            .accessibilityHint("Apre i filtri")
            FlowLayout {
                Pill(text: "\(found.listings.count) camper")
                if found.newCount > 0 {
                    Pill(text: found.newCount == 1 ? "1 nuovo" : "\(found.newCount) nuovi", background: .newPill, foreground: .newPillText)
                }
                Pill(text: "alle \(updatedAtText)")
            }
            if !found.unavailableSources.isEmpty {
                Warnings(sources: found.unavailableSources)
            }
        }
    }

    private var updatedAtText: String {
        let seconds = TimeInterval(found.updatedAt.toEpochMilliseconds()) / 1000
        return Date(timeIntervalSince1970: seconds).formatted(date: .omitted, time: .shortened)
    }
}

private struct Pill: View {
    let text: String
    var background: Color = .pill
    var foreground: Color = .secondaryText

    var body: some View {
        Text(text)
            .scaledFont(17)
            .foregroundStyle(foreground)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(background, in: Capsule())
    }
}

private struct Warnings: View {
    let sources: [String]

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Divider().overlay(Color.hairline)
            ForEach(sources, id: \.self) { source in
                Label("\(source) non è raggiungibile al momento", systemImage: "exclamationmark.circle")
                    .scaledFont(17)
                    .foregroundStyle(Color.warning)
                    .padding(.vertical, 10)
            }
            Divider().overlay(Color.hairline)
        }
    }
}

private struct EmptyResultsView: View {
    var body: some View {
        VStack(spacing: 20) {
            RoundIcon(systemName: "bus", background: .sageContainer, tint: .sage)
            Text("Oggi nessun camper nuovo corrisponde ai vostri criteri. Riprovate domani!")
                .scaledFont(22)
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, 36)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
