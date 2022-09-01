# taskapp - A smarter to-do list app.

taskapp is a to-do list app that balances your schedule to help reduce your stress by reducing the variance between the amount of committments per day. taskapp works by taking three extra fields compared to most to-do list apps - the earliest day you can complete the task, what tasks must be completed first, and how long the task should take to complete. Using these three extra fields, as well as the standard due date, taskapp schedules a "Do Date", which is the date between the earliest completion date and due date that A) has minimum variance, and B) comes after all it's prerequisite tasks. taskapp also keeps track of your calendar events - making sure to give you a lighter schedule on meeting-filled days.

## Schedule
<div align="center">
  <figure>
    <img src="https://i.imgur.com/QBExov8.png" alt="app's main screen showing daily schedule" width="200"/>
  </figure>
  <p>An example schedule in taskapp</p>
</div>

The schedule has all your tasks and events in a simple, uncluttered interface. Checking off/deleting tasks is easy, and adding a new event or task is just a tap away.

<div align="center">
  <figure>
    <img src="https://imgur.com/beLhKdM.png" alt="app's main screen in dark mode" width="200"/>
  </figure>
  <p>taskapp also supports Android system-wide dark mode</p>
</div>

## Add Task
<div align="center">
  <figure>
    <img src="https://imgur.com/kk8t5v8.png" alt="add task screen" width="200"/>
  </figure>
  <p>The add task screen</p>
</div>

Adding a task is easy in taskapp - you can get all the benefits of the optimizer with only 4 fields. Name is the name of the task, the earliest completion date is the earliest day you can complete the task, the due date is the latest day you can complete the task, time to complete is an estimate of how long the task will take, and prerequisite tasks are tasks you must finish before this one. Don't worry about having an accurate time to complete - as long as you stay in the ballpark the optimizer should still work well.

<div align="center">
  <figure>
    <img src="https://imgur.com/6TJqAKh.png" alt="a filled out task add screen" width="200"/>
  </figure>
  <p>The add task screen when filled out</p>
</div>

## Add Event

<div align="center">
  <figure>
    <img src="https://imgur.com/vEpjlMB.png" alt="the add event screen" width="200"/>
  </figure>
  <p>The add event screen</p>
</div>

Adding an event is easy, simply give the app the name of the event, the start time, and approximately how long the event will last.

<div align="center">
  <figure>
    <img src="https://imgur.com/rkqQ3Vs.png" alt="the recurrence screen" width="200"/>
  </figure>
  <p>The recurrence screen</p>
</div>

taskapp also provides advanced recurrence options - allowing you to set nearly every imaginable custom interval to recur on, such as this one that recurs every Monday, Wednesday, and Friday until December 14th.

<div align="center">
  <figure>
    <img src="https://imgur.com/Et5C9o1.png" alt="a completed add event screen" width="200"/>
  </figure>
  <p>The add event screen, when filled out</p>
</div>

## Javadoc
To help navigate the structure of this project, we've provided a [Javadoc](https://e-d-v.github.io/taskapp/).
