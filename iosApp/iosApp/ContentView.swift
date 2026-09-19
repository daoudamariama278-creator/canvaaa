import SwiftUI
#if canImport(ComposeApp)
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
#endif

struct ContentView: View {
    @State private var shipX: CGFloat = 0
    @State private var score: Int = 0
    @State private var isPlaying: Bool = true

    var body: some View {
        #if canImport(ComposeApp)
        ComposeView()
            .ignoresSafeArea(.all)
        #else
        ZStack {
            // Space background
            Color(red: 0.04, green: 0.05, blue: 0.12)
                .ignoresSafeArea()

            VStack(spacing: 24) {
                Spacer()

                // Game Title & Badge
                VStack(spacing: 8) {
                    Text("🚀 SPACE DODGER")
                        .font(.system(size: 32, weight: .black, design: .monospaced))
                        .foregroundColor(.cyan)

                    Text("Score: \(score)")
                        .font(.system(size: 22, weight: .bold, design: .rounded))
                        .foregroundColor(.yellow)
                }

                Spacer()

                // Interactive Ship
                Text("🛸")
                    .font(.system(size: 64))
                    .offset(x: shipX)
                    .gesture(
                        DragGesture()
                            .onChanged { value in
                                shipX = value.translation.width
                            }
                            .onEnded { _ in
                                withAnimation(.spring()) {
                                    shipX = 0
                                    score += 10
                                }
                            }
                    )

                Text("Glissez pour esquiver les astéroïdes !")
                    .font(.subheadline)
                    .foregroundColor(.white.opacity(0.8))

                Spacer()

                Button(action: {
                    withAnimation {
                        score += 50
                    }
                }) {
                    Text("Turbo Boost (+50)")
                        .font(.headline)
                        .foregroundColor(.white)
                        .padding(.horizontal, 28)
                        .padding(.vertical, 14)
                        .background(
                            LinearGradient(
                                colors: [Color.cyan, Color.blue],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                        .cornerRadius(25)
                        .shadow(color: .cyan.opacity(0.5), radius: 8, x: 0, y: 4)
                }

                Spacer()
            }
            .padding()
        }
        #endif
    }
}

