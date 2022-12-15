# Bubble-Picker

[![License](http://img.shields.io/badge/license-MIT-green.svg?style=flat)]()

<a href='https://play.google.com/store/apps/details?id=com.igalata.bubblepickerdemo&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' height="70" width="180"/></a>

Check this [project on dribbble](https://dribbble.com/shots/3349372-Bubble-Picker-Open-Source-Component)

Read how we did it [on Medium](https://medium.com/@igalata13/how-to-create-a-bubble-selection-animation-on-android-627044da4854#.ajonc010b)

<img src="shot.gif"/>

## Requirements
- Android SDK 16+
- Support run on fragment.
- Max target is 33.

## Usage

Add to your root build.gradle:
```Groovy
'Clone andadd module to project.'
```

Add the dependency:
```Groovy
'N/A.'
```

## How to use this library

Don't add `BubblePicker` to your xml layout. Please using it on code. Only init and add it to view.

```kotlin 
picker = BubblePicker(this.requireContext(), null)
view.add(picker)
```

Override onResume() and onPause() methods to call the same methods from the `BubblePicker`

Kotlin
```kotlin
override fun onResume() {
      super.onResume()
      picker.onResume()
}

override fun onPause() {
      super.onPause()
      picker.onPause()
}
```

Pass the `PickerItem` list to the `BubblePicker`

Kotlin
```kotlin
val titles = resources.getStringArray(R.array.countries)
val colors = resources.obtainTypedArray(R.array.colors)
val images = resources.obtainTypedArray(R.array.images)

picker.adapter = object : BubblePickerAdapter {

            override val totalCount = titles.size

            override fun getItem(position: Int): PickerItem {
                return PickerItem().apply {
                    title = titles[position]
                    gradient = BubbleGradient(colors.getColor((position * 2) % 8, 0),
                            colors.getColor((position * 2) % 8 + 1, 0), BubbleGradient.VERTICAL)
                    typeface = mediumTypeface
                    textColor = ContextCompat.getColor(this@DemoActivity, android.R.color.white)
                    backgroundImage = ContextCompat.getDrawable(this@DemoActivity, images.getResourceId(position, 0))
                }
            }
}
```

Specify the `BubblePickerListener` to get notified about events

Kotlin
```kotlin
picker.listener = object : BubblePickerListener {
            override fun onBubbleSelected(item: PickerItem) {

            }

            override fun onBubbleDeselected(item: PickerItem) {

            }
}
```

To get all selected items use `picker.selectedItems` variable in Kotlin or `picker.getSelectedItems()` method in Java.

For more usage examples please review the sample app

## Changelog

### Version: 1.0

* Added a possibility to setup the `BubblePicker` using `BubblePickerAdapter`

## Known iOS versions of the animation

* https://github.com/Ronnel/BubblePicker
* https://github.com/efremidze/Magnetic

## License

MIT License

Copyright (c) 2017 Irina Galata

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
