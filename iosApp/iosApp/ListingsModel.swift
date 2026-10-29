import Observation
import os
import Shared

@MainActor
@Observable
final class ListingsModel {
    private(set) var result: SearchResult?

    private let repository = CamperRepository()
    private let logger = Logger(subsystem: "camperte", category: "ListingsModel")

    func load() async {
        do {
            result = try await repository.loadListings()
        } catch {
            logger.error("Could not load listings: \(error)")
        }
    }

    func retry() async {
        result = nil
        await load()
    }
}
