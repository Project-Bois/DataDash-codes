import json
import platform
import os
import logging
from logging.handlers import QueueHandler
from PyQt6.QtCore import QThread, pyqtSignal
from queue import Queue


def get_logger_file_path():
    if platform.system() == 'Windows':
        logger_dir = os.path.join(os.getenv('LOCALAPPDATA'), 'Temp' ,  'DataDash')
    elif platform.system() == 'Linux':
        logger_dir = os.path.join(os.path.expanduser('~'), '.cache', 'DataDash')
    elif platform.system() == 'Darwin':  # macOS
        logger_dir = os.path.join(os.path.expanduser('~/Library/Caches'), 'DataDash')
    else:
        return None
    
    # Create the directory if it doesn't exist
    os.makedirs(logger_dir, exist_ok=True)
    return logger_dir


class LoggingThread(QThread):
    # Signal to emit logs if needed in GUI (optional)
    log_signal = pyqtSignal(str)

    def __init__(self, log_queue, log_file_path):
        super().__init__()
        self.log_queue = log_queue
        self.log_file_path = log_file_path
        self.running = True

    def run(self):
        # Configure the listener logger
        listener_logger = logging.getLogger('FileSharing:Listener')
        listener_logger.setLevel(logging.DEBUG)

        formatter = logging.Formatter('%(asctime)s %(levelname)s %(name)s %(message)s')

        # File handler
        file_handler = logging.FileHandler(self.log_file_path, mode='a')
        file_handler.setLevel(logging.DEBUG)
        file_handler.setFormatter(formatter)

        # Console handler
        console_handler = logging.StreamHandler()
        console_handler.setLevel(logging.DEBUG)
        console_handler.setFormatter(formatter)

        listener_logger.addHandler(file_handler)
        listener_logger.addHandler(console_handler)

        while self.running:
            try:
                record = self.log_queue.get(timeout=0.1)
                listener_logger.handle(record)
            except Exception:
                continue

    def stop(self):
        self.running = False
        self.quit()
        self.wait()

# Set up logging queue and thread
log_queue = Queue()
log_dir = get_logger_file_path()
if log_dir is None:
    raise RuntimeError("Unsupported OS!")

log_file_path = os.path.join(log_dir, 'datadashlog.txt')

logging_thread = LoggingThread(log_queue, log_file_path)
logging_thread.start()

# Configure the main logger to use QueueHandler
logger = logging.getLogger('FileSharing: ')
logger.setLevel(logging.DEBUG)
queue_handler = QueueHandler(log_queue)
logger.addHandler(queue_handler)

# Optional: Clean up logging_thread when the application exits
def stop_logging_thread():
    logging_thread.stop()

# Register the cleanup function if needed


