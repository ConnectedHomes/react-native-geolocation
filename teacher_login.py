#!/usr/bin/env python3
"""
Tkinter Teacher Login Form with Validation and UI Enhancements
"""
import tkinter as tk
from tkinter import ttk

# Hardcoded credentials for validation
# NOTE: For production use, replace with secure authentication backend
VALID_ID = "teacher123"
VALID_PASSWORD = "password123"

# UI Color constants
BG_COLOR = "#f0f0f0"
TEXT_COLOR = "#333333"
BUTTON_BG_COLOR = "#4CAF50"
BUTTON_FG_COLOR = "white"
FOOTER_COLOR = "#666666"
BORDER_COLOR = "gray"

# Canvas circle coordinates
OUTER_CIRCLE_START = 5
OUTER_CIRCLE_END = 45
INNER_CIRCLE_START = 8
INNER_CIRCLE_END = 42


def login():
    """
    Validates teacher credentials and displays appropriate feedback message.
    """
    teacher_id = teacher_id_entry.get().strip()
    password = password_entry.get().strip()
    
    # Check if fields are empty
    if not teacher_id or not password:
        show_message("Please enter ID and Password", "red")
        return
    
    # Validate credentials
    # Security note: Generic error message prevents username enumeration attacks
    if teacher_id == VALID_ID and password == VALID_PASSWORD:
        show_message("Login successful", "green")
    else:
        show_message("Invalid ID or Password", "red")


def show_message(text, color):
    """
    Displays a message with the specified color.
    
    Args:
        text: The message text to display
        color: The color of the message text
    """
    message_label.config(text=text, foreground=color)


def update_tick_mark(event=None):
    """
    Updates the tick mark canvas to show a green or red circle based on ID entry.
    NOTE: Green circle indicates ID field has content (not that ID is valid).
    Red circle indicates the field is empty.
    
    Args:
        event: The event that triggered this function (optional)
    """
    tick_canvas.delete("all")
    
    # Draw outer circle border
    tick_canvas.create_oval(
        OUTER_CIRCLE_START, OUTER_CIRCLE_START,
        OUTER_CIRCLE_END, OUTER_CIRCLE_END,
        outline=BORDER_COLOR, width=2
    )
    
    # Draw inner circle based on whether ID field has content
    if teacher_id_entry.get().strip():
        tick_canvas.create_oval(
            INNER_CIRCLE_START, INNER_CIRCLE_START,
            INNER_CIRCLE_END, INNER_CIRCLE_END,
            fill="green", outline="green", width=2
        )
    else:
        tick_canvas.create_oval(
            INNER_CIRCLE_START, INNER_CIRCLE_START,
            INNER_CIRCLE_END, INNER_CIRCLE_END,
            fill="red", outline="red", width=2
        )


# Create main window
root = tk.Tk()
root.title("Teacher Login")
root.geometry("450x400")
root.resizable(False, False)
root.configure(bg=BG_COLOR)

# Title Label
title_label = tk.Label(
    root,
    text="Teacher Login",
    font=("Arial", 24, "bold"),
    bg=BG_COLOR,
    fg=TEXT_COLOR
)
title_label.pack(pady=20)

# Credentials Frame
credentials_frame = tk.Frame(root, bg=BG_COLOR)
credentials_frame.pack(pady=10)

# Teacher ID Label and Entry
teacher_id_label = tk.Label(
    credentials_frame,
    text="Teacher ID:",
    font=("Arial", 14),
    bg=BG_COLOR
)
teacher_id_label.grid(row=0, column=0, sticky="w", padx=10, pady=10)

teacher_id_entry = tk.Entry(
    credentials_frame,
    font=("Arial", 14),
    width=20,
    relief="solid",
    borderwidth=1
)
teacher_id_entry.grid(row=0, column=1, padx=10, pady=10)

# Bind KeyRelease event to update tick mark in real-time
teacher_id_entry.bind("<KeyRelease>", update_tick_mark)

# Tick Mark Canvas
tick_canvas = tk.Canvas(
    credentials_frame,
    width=50,
    height=50,
    bg=BG_COLOR,
    highlightthickness=0
)
tick_canvas.grid(row=0, column=2, padx=10, pady=10)

# Password Label and Entry
password_label = tk.Label(
    credentials_frame,
    text="Password:",
    font=("Arial", 14),
    bg=BG_COLOR
)
password_label.grid(row=1, column=0, sticky="w", padx=10, pady=10)

password_entry = tk.Entry(
    credentials_frame,
    font=("Arial", 14),
    width=20,
    show="*",
    relief="solid",
    borderwidth=1
)
password_entry.grid(row=1, column=1, padx=10, pady=10)

# Bind Enter key to trigger login for better UX
password_entry.bind("<Return>", lambda event: login())

# Login Button
login_button = tk.Button(
    credentials_frame,
    text="Log In",
    command=login,
    font=("Arial", 14, "bold"),
    bg=BUTTON_BG_COLOR,
    fg=BUTTON_FG_COLOR,
    width=15,
    height=2,
    relief="raised",
    cursor="hand2"
)
login_button.grid(row=2, column=0, columnspan=3, pady=20)

# Message Label for feedback
message_label = ttk.Label(
    root,
    text="",
    font=("Arial", 12),
    background=BG_COLOR
)
message_label.pack(pady=10)

# Contact Information Frame
contact_frame = tk.Frame(root, bg=BG_COLOR)
contact_frame.pack(side="bottom", pady=20)

contact_label = tk.Label(
    contact_frame,
    text="For assistance, contact: support@school.edu | Phone: (555) 123-4567",
    font=("Arial", 10),
    bg=BG_COLOR,
    fg=FOOTER_COLOR
)
contact_label.pack()

# Initialize tick mark on startup (red circle for empty ID field)
update_tick_mark()

# Start the GUI event loop
root.mainloop()
