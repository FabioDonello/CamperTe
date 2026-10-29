import Shared
import SwiftUI

struct ListingCardView: View {
    let shown: ShownListing

    @Environment(\.openURL) private var openURL

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            PhotoArea(shown: shown)
            VStack(alignment: .leading, spacing: 10) {
                Text(shown.listing.title)
                    .scaledFont(20, weight: .medium)
                if !shown.detailsText.isEmpty {
                    Text(shown.detailsText)
                        .scaledFont(18)
                        .foregroundStyle(Color.secondaryText)
                }
                if !shown.bonusWords.isEmpty {
                    FlowLayout {
                        ForEach(shown.bonusWords, id: \.self) { BonusChip(word: $0) }
                    }
                }
                Button(action: open) {
                    HStack(spacing: 8) {
                        Text("Vedi annuncio")
                        Image(systemName: "arrow.right")
                    }
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(ActionButtonStyle())
                .padding(.top, 4)
            }
            .padding(EdgeInsets(top: 14, leading: 18, bottom: 18, trailing: 18))
        }
        .background(Color.cardBackground)
        .clipShape(RoundedRectangle(cornerRadius: 24))
        .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color.hairline))
    }

    private func open() {
        guard let url = URL(string: shown.listing.url) else { return }
        openURL(url)
    }
}

private struct PhotoArea: View {
    let shown: ShownListing

    var body: some View {
        Color.pill
            .frame(height: 240)
            .overlay { photo }
            .clipped()
            .overlay(alignment: .bottomLeading) {
                if let price = shown.priceText {
                    PriceOverlay(price: price)
                }
            }
            .overlay(alignment: .topLeading) {
                if shown.isNew {
                    NewBadge().padding(14)
                }
            }
    }

    @ViewBuilder
    private var photo: some View {
        if let photoUrl = shown.listing.photoUrl, let url = URL(string: photoUrl) {
            AsyncImage(url: url) { phase in
                if let image = phase.image {
                    image.resizable().scaledToFill()
                } else if phase.error != nil {
                    PhotoUnavailable()
                }
            }
            .accessibilityLabel(shown.listing.title)
        } else {
            PhotoUnavailable()
        }
    }
}

private struct PhotoUnavailable: View {
    var body: some View {
        Text("Foto non disponibile")
            .scaledFont(18)
            .foregroundStyle(Color.secondaryText)
            .padding(.bottom, 40)
    }
}

private struct PriceOverlay: View {
    let price: String

    var body: some View {
        Text(price)
            .scaledFont(30, weight: .bold, relativeTo: .title)
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(EdgeInsets(top: 28, leading: 18, bottom: 12, trailing: 18))
            .background(LinearGradient(colors: [.clear, .black.opacity(0.75)], startPoint: .top, endPoint: .bottom))
    }
}

private struct NewBadge: View {
    var body: some View {
        Text("NUOVO")
            .scaledFont(15, weight: .bold)
            .tracking(0.9)
            .foregroundStyle(.white)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(Color.newBadge, in: Capsule())
    }
}

private struct BonusChip: View {
    let word: String

    var body: some View {
        Label {
            Text(word)
        } icon: {
            Image(systemName: "checkmark").fontWeight(.bold).imageScale(.small)
        }
        .scaledFont(16)
        .foregroundStyle(Color.chipText)
        .padding(.horizontal, 11)
        .padding(.vertical, 4)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.chipBorder))
    }
}
