import Observation
import os
import Shared

struct FiltersEditing {
    var filters: SearchFilters
    var locationDenied: Bool
    var pendingDistanceKm: Int?
}

@MainActor
@Observable
final class ListingsModel {
    private(set) var result: SearchResult?
    private(set) var filtersSummary = ""
    private(set) var editing: FiltersEditing?

    private let repository = CamperRepository()
    private let location = LocationProvider()
    private let logger = Logger(subsystem: "camperte", category: "ListingsModel")

    func load() async {
        let position = await location.currentPosition()
        do {
            result = try await repository.loadListings(position: position)
            filtersSummary = repository.filtersSummary(filters: repository.currentFilters(), hasPosition: position != nil)
        } catch {
            logger.error("Could not load listings: \(error)")
        }
    }

    func retry() async {
        result = nil
        await load()
    }

    func openFilters() {
        editing = FiltersEditing(filters: repository.currentFilters(), locationDenied: location.isDenied, pendingDistanceKm: nil)
    }

    func edit(_ change: (SearchFilters) -> SearchFilters) {
        guard let current = editing else { return }
        editing?.filters = change(current.filters)
    }

    func resetFilters() {
        edit { _ in repository.defaultFilters }
    }

    func chooseArea(_ area: SearchArea) {
        if let nearby = area as? SearchAreaNearby, !location.hasPermission {
            editing?.pendingDistanceKm = Int(nearby.km)
        } else {
            edit { $0.withArea(area: area) }
        }
    }

    func allowLocation() async {
        guard let km = editing?.pendingDistanceKm else { return }
        let granted = await location.requestPermission()
        editing?.pendingDistanceKm = nil
        if granted {
            edit { $0.withArea(area: SearchAreaNearby(km: Int32(km))) }
        } else {
            editing?.locationDenied = true
        }
    }

    func dismissLocationRequest() {
        editing?.pendingDistanceKm = nil
    }

    func refreshLocationState() {
        editing?.locationDenied = location.isDenied
    }

    func closeFilters() async {
        guard let edited = editing?.filters else { return }
        editing = nil
        if edited != repository.currentFilters() {
            repository.saveFilters(filters: edited)
            result = nil
            await load()
        }
    }
}
