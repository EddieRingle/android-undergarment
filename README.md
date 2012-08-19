Undergarment
============

A slide-out navigation (a.k.a. "Drawer") implementation for Android applications.  

The slide-out navigation pattern has been popping up in all sorts of places in the Android
ecosystem lately in all sorts of shapes and sizes. Undergarment initially started with me
mimicking the experience found in the Prixing application, as documented by Cyril Mottier
in his series of [three](http://android.cyrilmottier.com/?p=658)
[blog](http://android.cyrilmottier.com/?p=701) [posts](http://android.cyrilmottier.com/?p=717)
on the subject.  

Just recently (August 2012), the Android team updated their Design site specifically with a new
pattern documenting the slide-out navigation every developer and their dog had been
playing around with to that date. Thusly, the "Drawer" pattern was born. You can find the
Android team's documentation regarding the Drawer pattern under the
["View Controls" section](http://developer.android.com/design/patterns/actionbar.html#elements)
of the Action Bar patterns page.  

Now, as the Prixing implementation dealt with sliding the entire window (Action Bar and all)
over to the side in order to reveal the "drawer" while the pattern on the Android Design site
used the YouTube app as a reference, which slides only the content portion over and keeps the
Action Bar stationary, my implementation was a bit lacking. So, I added support for sliding
just the content portion over, moved the implementation into a standalone Android Library
project, and put it on GitHub. That's the story of how Undergarment was born.  

Using & Such
------------

I'm a fan of git submodules (and IntelliJ), so in order to use this library I recommend adding
this repository as a submodule of your encompassing project's repository:  

    git submodule add git://github.com/eddieringle/android-undergarment contrib/android-undergarment

Now add the Undergarment library as a module in IntelliJ and add that module as a dependency of
your project. You're then ready to start using Undergarment!  

Integrating Undergarment is fairly straightforward. In the onCreate of the Activity hosting
the Undergarment (a.k.a. "Drawer") do a little something like this:  

    mDrawerGarment = new DrawerGarment(this, R.layout.dashboard);

The constructor takes two arguments, the first being the Activity that hosts the Undergarment
and the second being a resource identifier to the layout that will define the drawer contents.  

By default, Undergarment is set to slide the entire window over, Action Bar and all. If you wish
to switch to the YouTube-style of only sliding over the content then you can set it like so:  

    mDrawerGarment.setSlideTarget(DrawerGarment.SLIDE_TARGET_CONTENT);

To open and close the drawer, call the `openDrawer()` and `closeDrawer()` methods,
respectively. You can also toggle the drawer state with the `toggleDrawer()` method. The user
will also be able to control the drawer by bezel swiping from the left side of the screen to
open the drawer and doing the same from the right to close it. If you want to prevent this
touch functionality, you can call `setDrawerEnabled(false)`.  

Finally, you can provide callbacks to the drawer that will fire when the drawer is opened or
closed, which allows you to do useful things such as modifying the state of the Action Bar.
To set the callbacks, implement the `DrawerGarment.IDrawerCallbacks` interface and add them to
the drawer with `setDrawerCallbacks(callbacks)`.