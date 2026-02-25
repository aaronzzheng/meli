# meli Android Guide

This guide shows how to add a new page (`Fragment`), wire buttons to navigate between pages, and build/move UI elements in a `ConstraintLayout`.

## 1. Create a new Fragment/page

1. Create files:
   - Kotlin: `app/src/main/java/com/example/meli/ui/<feature>/<Feature>Fragment.kt`
   - ViewModel (optional but recommended): `.../<Feature>ViewModel.kt`
   - Layout: `app/src/main/res/layout/fragment_<feature>.xml`

2. Use View Binding in the fragment (same pattern used in this project):

```kotlin
class FeatureFragment : Fragment() {
    private var _binding: FragmentFeatureBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeatureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

## 2. Register the page in Navigation

Edit `app/src/main/res/navigation/mobile_navigation.xml` and add a fragment destination:

```xml
<fragment
    android:id="@+id/featureFragment"
    android:name="com.example.meli.ui.feature.FeatureFragment"
    android:label="@string/title_feature"
    tools:layout="@layout/fragment_feature" />
```

If navigating from another destination, add an action under the source fragment:

```xml
<action
    android:id="@+id/action_navigation_home_to_featureFragment"
    app:destination="@id/featureFragment" />
```

## 3. Link a button to open a page

1. Add button in source layout (example `fragment_home.xml`):

```xml
<Button
    android:id="@+id/button_open_feature"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@string/open_feature"
    app:layout_constraintTop_toBottomOf="@id/text_home"
    app:layout_constraintStart_toStartOf="parent" />
```

2. Handle click in source fragment:

```kotlin
binding.buttonOpenFeature.setOnClickListener {
    findNavController().navigate(R.id.action_navigation_home_to_featureFragment)
}
```

Notes:
- `android:id` in XML `button_open_feature` becomes `binding.buttonOpenFeature`.
- Action ID in `navigate(...)` must exist in `mobile_navigation.xml`.

## 4. Add a button/text/image and move it around (ConstraintLayout)

In `ConstraintLayout`, position comes from constraints, not absolute coordinates.

1. Add view:

```xml
<TextView
    android:id="@+id/text_feature_title"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:text="@string/title_feature"
    app:layout_constraintTop_toTopOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintEnd_toEndOf="parent" />
```

2. In Android Studio **Design** tab:
   - Drag the view.
   - Create constraints to parent or sibling (top/start/end/bottom).
   - Adjust margins and bias to fine-tune position.

3. Parent/sibling linking examples:
   - To parent: `app:layout_constraintStart_toStartOf="parent"`
   - Below another view: `app:layout_constraintTop_toBottomOf="@id/text_feature_title"`
   - Align ends: `app:layout_constraintEnd_toEndOf="@id/someOtherView"`

Important:
- Avoid relying on large fixed margins for placement.
- Ensure each view has enough constraints (at least one horizontal + one vertical chain/anchor).

## 5. Add to bottom nav (optional)

If the page should be in bottom navigation:

1. Add item in `app/src/main/res/menu/bottom_nav_menu.xml`:

```xml
<item
    android:id="@+id/featureFragment"
    android:icon="@drawable/ic_feature"
    android:title="@string/title_feature" />
```

2. Include its destination ID in `MainActivity` visibility logic if needed.

## 6. Strings and accessibility (always do this)

1. Put visible text in `app/src/main/res/values/strings.xml`.
2. Add `android:contentDescription` for clickable images (`ImageButton`, `ImageView`, FAB).
3. Use meaningful IDs:
   - Good: `button_settings`, `text_profile_title`
   - Avoid: `button1`, `textView2`

## 7. Quick checklist before commit

1. New fragment class + layout exists.
2. Destination/action added in `mobile_navigation.xml`.
3. Click listener calls the correct action ID.
4. All new UI views are constrained correctly.
5. New strings added to `strings.xml`.
6. Build passes:
   - `./gradlew :app:assembleDebug`
   - Optional: `./gradlew :app:lintDebug`
