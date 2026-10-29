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

    @State private var page = 0

    private var photos: [String] { shown.listing.photoUrls }

    var body: some View {
        Color.pill
            .frame(height: 240)
            .overlay { gallery }
            .clipped()
            .overlay(alignment: .bottomLeading) {
                BottomOverlay(price: shown.priceText, distance: shown.distanceText, page: page, photoCount: photos.count)
            }
            .overlay(alignment: .topLeading) {
                if shown.isNew {
                    NewBadge().padding(14)
                }
            }
            .overlay(alignment: .topTrailing) {
                if photos.count > 1 {
                    PhotoCounter(position: page + 1, total: photos.count).padding(14)
                }
            }
    }

    @ViewBuilder
    private var gallery: some View {
        if photos.isEmpty {
            PhotoUnavailable()
        } else {
            TabView(selection: $page) {
                ForEach(Array(photos.enumerated()), id: \.offset) { index, url in
                    Photo(url: url, title: shown.listing.title).tag(index)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
        }
    }
}

private struct Photo: View {
    let url: String
    let title: String

    var body: some View {
        AsyncImage(url: URL(string: url)) { phase in
            if let image = phase.image {
                image.resizable().scaledToFill()
            } else if phase.error != nil {
                PhotoUnavailable()
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
        .accessibilityLabel(title)
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

private struct PhotoCounter: View {
    let position: Int
    let total: Int

    var body: some View {
        Label("\(position) / \(total)", systemImage: "camera")
            .scaledFont(16, weight: .medium)
            .foregroundStyle(.white)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(.black.opacity(0.62), in: Capsule())
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Foto \(position) di \(total)")
    }
}

private struct BottomOverlay: View {
    let price: String?
    let distance: String?
    let page: Int
    let photoCount: Int

    private static let maxDots = 8

    private var showsDots: Bool { (2...Self.maxDots).contains(photoCount) }

    var body: some View {
        if price != nil || distance != nil || showsDots {
            HStack(alignment: .bottom) {
                Text(price ?? "")
                    .scaledFont(30, weight: .bold, relativeTo: .title)
                Spacer()
                VStack(alignment: .trailing, spacing: 8) {
                    if let distance {
                        Text(distance).scaledFont(17, weight: .medium)
                    }
                    if showsDots {
                        PageDots(current: page, count: photoCount).padding(.bottom, 6)
                    }
                }
            }
            .foregroundStyle(.white)
            .padding(EdgeInsets(top: 28, leading: 18, bottom: 12, trailing: 18))
            .background(LinearGradient(colors: [.clear, .black.opacity(0.75)], startPoint: .top, endPoint: .bottom))
        }
    }
}

private struct PageDots: View {
    let current: Int
    let count: Int

    var body: some View {
        HStack(spacing: 6) {
            ForEach(0..<count, id: \.self) { index in
                Circle()
                    .fill(.white.opacity(index == current ? 1 : 0.45))
                    .frame(width: 9, height: 9)
            }
        }
        .accessibilityHidden(true)
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
