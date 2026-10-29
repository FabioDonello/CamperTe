import SwiftUI

@main
struct CamperTeApp: App {
    @State private var model = ListingsModel()

    var body: some Scene {
        WindowGroup {
            ListingsView(model: model)
        }
    }
}
